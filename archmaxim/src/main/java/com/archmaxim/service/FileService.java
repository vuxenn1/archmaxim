package com.archmaxim.service;

import com.archmaxim.config.ArchmaximProperties;
import com.archmaxim.dto.FileDto;
import com.archmaxim.dto.PageResponse;
import com.archmaxim.dto.UploadResponse;
import com.archmaxim.entity.Course;
import com.archmaxim.entity.FileRecord;
import com.archmaxim.entity.FileType;
import com.archmaxim.entity.Lecturer;
import com.archmaxim.entity.Role;
import com.archmaxim.entity.Season;
import com.archmaxim.exception.BadRequestException;
import com.archmaxim.exception.ForbiddenException;
import com.archmaxim.exception.NotFoundException;
import com.archmaxim.repository.CourseLecturerRepository;
import com.archmaxim.repository.CourseRepository;
import com.archmaxim.repository.FileRecordRepository;
import com.archmaxim.repository.LecturerRepository;
import com.archmaxim.search.IndexingService;
import com.archmaxim.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Year;
import java.util.Locale;
import java.util.Set;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final int FIRST_YEAR = 2000;
    private static final int MAX_NAME_LENGTH = 255;

    private final FileRecordRepository fileRepository;
    private final CourseRepository courseRepository;
    private final LecturerRepository lecturerRepository;
    private final CourseLecturerRepository assignmentRepository;
    private final FileStorageService storage;
    private final IndexingService indexingService;
    private final TransactionTemplate transaction;
    private final Set<String> allowedExtensions;
    private final Clock clock;

    public FileService(FileRecordRepository fileRepository,
                       CourseRepository courseRepository,
                       LecturerRepository lecturerRepository,
                       CourseLecturerRepository assignmentRepository,
                       FileStorageService storage,
                       IndexingService indexingService,
                       PlatformTransactionManager transactionManager,
                       ArchmaximProperties properties,
                       Clock clock) {
        this.fileRepository = fileRepository;
        this.courseRepository = courseRepository;
        this.lecturerRepository = lecturerRepository;
        this.assignmentRepository = assignmentRepository;
        this.storage = storage;
        this.indexingService = indexingService;
        this.transaction = new TransactionTemplate(transactionManager);
        this.allowedExtensions = Set.copyOf(properties.upload().allowedExtensions());
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<FileDto> findPage(int page, int size) {
        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return PageResponse.of(fileRepository.findByActiveTrue(request), FileDto::from);
    }

    /**
     * Saves the file and its metadata, then tries to index it. A failed save removes
     * the stored content again; a failed index only means search picks it up later.
     */
    public UploadResponse upload(MultipartFile file, FileType type, Long courseId, int year, Season season,
                                 String description, AuthenticatedUser user) {
        if (file.isEmpty()) {
            throw new BadRequestException("The file is empty.");
        }
        String originalName = cleanFilename(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!allowedExtensions.contains(extension)) {
            throw new BadRequestException(extension.isEmpty()
                    ? "Files without an extension are not allowed."
                    : "Files of type ." + extension + " are not allowed.");
        }
        int lastYear = Year.now(clock).getValue() + 1;
        if (year < FIRST_YEAR || year > lastYear) {
            throw new BadRequestException("The year must be between " + FIRST_YEAR + " and " + lastYear + ".");
        }

        Course course = courseRepository.findById(courseId).orElseThrow(() -> NotFoundException.of("Course", courseId));
        if (!assignmentRepository.existsByCourseIdAndLecturerId(courseId, user.id())) {
            throw new ForbiddenException("You are not assigned to " + course.getCode() + ".");
        }
        Lecturer lecturer = lecturerRepository.findById(user.id())
                .orElseThrow(() -> new ForbiddenException("Your lecturer account no longer exists."));

        String storedName = storage.store(file);
        FileRecord saved;
        try {
            saved = transaction.execute(status -> fileRepository.save(new FileRecord(storedName, originalName, type,
                    course, lecturer, year, season, file.getSize(), file.getContentType(), blankToNull(description))));
        } catch (RuntimeException e) {
            storage.deleteQuietly(storedName);
            throw e;
        }
        log.info("{} uploaded file {} ({}) to {}", user.email(), saved.getId(), originalName, course.getCode());

        boolean indexed = indexingService.indexNow(saved.getId());
        FileRecord detailed = fileRepository.findWithDetailsById(saved.getId()).orElseThrow();
        return new UploadResponse(FileDto.from(detailed), indexed);
    }

    /**
     * Returns the file content and counts the download. The count only goes up
     * once the content is known to be readable.
     */
    public FileDownload download(Long id) {
        FileRecord file = fileRepository.findByIdAndActiveTrue(id).orElseThrow(() -> NotFoundException.of("File", id));
        Path path = storage.resolve(file.getUuid());
        if (!Files.isReadable(path)) {
            log.warn("Content of file {} is missing at {}", id, path);
            throw new NotFoundException("The content of file " + id + " is missing.");
        }
        long size;
        try {
            size = Files.size(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read file " + id, e);
        }
        fileRepository.incrementDownloadCount(id);
        return new FileDownload(new FileSystemResource(path), file.getOriginalName(), mediaTypeOf(file), size);
    }

    @Transactional
    public void deactivate(Long id, AuthenticatedUser user) {
        FileRecord file = fileRepository.findByIdAndActiveTrue(id).orElseThrow(() -> NotFoundException.of("File", id));
        boolean owner = user.is(Role.LECTURER) && file.getUploadedBy().getId().equals(user.id());
        if (!user.is(Role.ADMIN) && !owner) {
            throw new ForbiddenException("You can only remove files you uploaded.");
        }
        file.deactivate();
        log.info("{} removed file {}", user.email(), id);
    }

    /**
     * Keeps only the last path segment (some browsers send full client paths)
     * and drops control characters.
     */
    static String cleanFilename(String filename) {
        if (filename == null) {
            return "file";
        }
        String name = filename.substring(Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\')) + 1);
        name = name.replaceAll("\\p{Cntrl}", "").strip();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            return "file";
        }
        if (name.length() > MAX_NAME_LENGTH) {
            String extension = extensionOf(name);
            int keep = MAX_NAME_LENGTH - (extension.isEmpty() ? 0 : extension.length() + 1);
            name = name.substring(0, keep) + (extension.isEmpty() ? "" : "." + extension);
        }
        return name;
    }

    static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 && dot < filename.length() - 1 ? filename.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private static MediaType mediaTypeOf(FileRecord file) {
        if (file.getMimeType() == null || file.getMimeType().isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(file.getMimeType());
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
