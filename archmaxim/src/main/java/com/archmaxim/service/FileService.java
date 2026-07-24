package com.archmaxim.service;

import com.archmaxim.entity.*;
import com.archmaxim.repository.CourseAliasRepository;
import com.archmaxim.repository.CourseRepository;
import com.archmaxim.repository.FileRepository;
import com.archmaxim.repository.LecturerRepository;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private final FileRepository fileRepository;
    private final CourseRepository courseRepository;
    private final LecturerRepository lecturerRepository;
    private final CourseAliasRepository courseAliasRepository;

    private final FileStorageService fileStorageService;

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingClient embeddingClient;

    public FileService(FileRepository fileRepository,
                       CourseRepository courseRepository,
                       LecturerRepository lecturerRepository,
                       CourseAliasRepository courseAliasRepository,
                       FileStorageService fileStorageService,
                       EmbeddingClient embeddingClient,
                       JdbcTemplate jdbcTemplate) {
        this.fileRepository = fileRepository;
        this.courseRepository = courseRepository;
        this.lecturerRepository = lecturerRepository;
        this.courseAliasRepository = courseAliasRepository;
        this.fileStorageService = fileStorageService;
        this.embeddingClient = embeddingClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<File> getAllFiles() {
        return fileRepository.findAll();
    }

    public File createFile(String originalName, FileType type, Long courseId, Long uploadedById, Integer year, Season season, String description) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));
        Lecturer uploadedBy = lecturerRepository.findById(uploadedById)
                .orElseThrow(() -> new RuntimeException("Lecturer not found: " + uploadedById));

        String uuid = UUID.randomUUID().toString();

        File file = new File(uuid, originalName, type, course, uploadedBy, year, season, null, null, description);
        return fileRepository.save(file);
    }

    public File uploadFile(MultipartFile multipartFile, FileType type, Long courseId, String uploaderEmail,
                           Integer year, Season season, String description) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));
        Lecturer uploadedBy = lecturerRepository.findByEmail(uploaderEmail)
                .orElseThrow(() -> new RuntimeException("Lecturer not found: " + uploaderEmail));

        String uuid = fileStorageService.store(multipartFile);

        File file = new File(uuid, multipartFile.getOriginalFilename(), type, course, uploadedBy,
                year, season, multipartFile.getSize(), multipartFile.getContentType(), description);

        File saved = fileRepository.save(file);
        indexFile(saved);
        return saved;
    }

    private String buildSearchableText(File file) {
        return String.join(" ",
                file.getCourse().getName(),
                file.getCourse().getCode(),
                file.getType().name(),
                turkishForType(file.getType()),
                file.getUploadedBy().getFirstName(),
                file.getUploadedBy().getLastName(),
                String.valueOf(file.getYear()),
                file.getSeason().name(),
                turkishForSeason(file.getSeason())
        );
    }

    public void indexFile(File file) {
        String text = buildSearchableText(file);
        float[] vector = embeddingClient.embed(text);

        String vectorLiteral = Arrays.toString(vector);   // "[0.12, -0.83, ...]"

        jdbcTemplate.update(
                "UPDATE file_record SET embedding = ?::vector WHERE id = ?",
                vectorLiteral,
                file.getId()
        );
    }

    public int indexAllFiles() {
        List<File> files = fileRepository.findAll();
        for (File file : files) {
            indexFile(file);
        }
        return files.size();
    }

    public List<File> search(String query, int limit,
                             Integer year, Season season, FileType type, Long departmentId) {

        // Enrich the query: aliases first (exact codes), then fuzzy (typos)
        String enrichedQuery = query;

        String aliasMatch = findAliasMatch(query);
        if (aliasMatch != null) {
            enrichedQuery = query + " " + aliasMatch;
        } else {
            String courseMatch = findBestCourseMatch(query);
            if (courseMatch != null) {
                enrichedQuery = query + " " + courseMatch;
            }
        }

        float[] queryVector = embeddingClient.embed(enrichedQuery);
        String vectorLiteral = Arrays.toString(queryVector);

        StringBuilder sql = new StringBuilder(
                "SELECT f.id FROM file_record f " +
                        "JOIN course c ON f.course_id = c.id " +
                        "WHERE f.embedding IS NOT NULL AND f.is_active = true " +
                        "AND (f.embedding <=> ?::vector) < ? ");

        List<Object> params = new ArrayList<>();
        params.add(vectorLiteral);   // for the threshold check
        params.add(0.6);             // threshold value, will be tuned

        if (year != null) {
            sql.append("AND f.year = ? ");
            params.add(year);
        }
        if (season != null) {
            sql.append("AND f.season = ? ");
            params.add(season.name());
        }
        if (type != null) {
            sql.append("AND f.type = ? ");
            params.add(type.name());
        }
        if (departmentId != null) {
            sql.append("AND c.department_id = ? ");
            params.add(departmentId);
        }

        sql.append("ORDER BY f.embedding <=> ?::vector LIMIT ?");
        params.add(vectorLiteral);
        params.add(limit);

        List<Long> ids = jdbcTemplate.queryForList(sql.toString(), Long.class, params.toArray());

        List<File> results = new ArrayList<>();
        for (Long id : ids) {
            fileRepository.findById(id).ifPresent(results::add);
        }
        return results;
    }

    private String turkishForType(FileType type) {
        return switch (type) {
            case MIDTERM  -> "vize ara sınav";
            case FINAL    -> "final final sınavı";
            case QUIZ     -> "quiz küçük sınav";
            case HOMEWORK -> "ödev";
            case NOTE     -> "ders notu notlar";
            case SYLLABUS -> "izlence müfredat";
            case LAB      -> "laboratuvar lab";
            case PROJECT  -> "proje";
            case OTHER    -> "diğer";
        };
    }

    private String turkishForSeason(Season season) {
        return switch (season) {
            case FALL   -> "güz sonbahar";
            case SPRING -> "bahar ilkbahar";
            case SUMMER -> "yaz";
        };
    }

    public File getFileForDownload(Long id) {
        File file = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found: " + id));
        file.setDownloadCount(file.getDownloadCount() + 1);
        return fileRepository.save(file);
    }

    private String findBestCourseMatch(String query) {
        JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();
        String queryLower = query.toLowerCase();

        String bestMatch = null;
        double bestScore = 0.0;

        for (Course course : courseRepository.findAll()) {
            // compare against both the course name and its code
            double nameScore = similarity.apply(queryLower, course.getName().toLowerCase());
            double codeScore = similarity.apply(queryLower, course.getCode().toLowerCase());
            double score = Math.max(nameScore, codeScore);

            if (score > bestScore) {
                bestScore = score;
                bestMatch = course.getName();
            }
        }

        // only accept the match if it's confidently close
        if (bestScore >= 0.7) {
            return bestMatch;
        }
        return null;
    }

    private String findAliasMatch(String query) {
        String queryLower = query.toLowerCase();
        for (CourseAlias alias : courseAliasRepository.findAll()) {
            // check if the query contains this alias as a whole word
            String aliasLower = alias.getAlias().toLowerCase();
            if (queryLower.contains(aliasLower)) {
                return alias.getCourse().getName();
            }
        }
        return null;
    }
}