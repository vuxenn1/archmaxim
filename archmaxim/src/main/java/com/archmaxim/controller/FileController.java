package com.archmaxim.controller;

import com.archmaxim.dto.FileDto;
import com.archmaxim.dto.FileSearchRequest;
import com.archmaxim.dto.IndexJobResponse;
import com.archmaxim.dto.IndexStatus;
import com.archmaxim.dto.PageResponse;
import com.archmaxim.dto.UploadResponse;
import com.archmaxim.entity.FileType;
import com.archmaxim.entity.Season;
import com.archmaxim.search.IndexingService;
import com.archmaxim.search.SearchService;
import com.archmaxim.security.AuthenticatedUser;
import com.archmaxim.service.FileDownload;
import com.archmaxim.service.FileService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;
    private final SearchService searchService;
    private final IndexingService indexingService;

    public FileController(FileService fileService, SearchService searchService, IndexingService indexingService) {
        this.fileService = fileService;
        this.searchService = searchService;
        this.indexingService = indexingService;
    }

    @GetMapping
    public PageResponse<FileDto> getPage(@RequestParam(defaultValue = "0") @Min(0) int page,
                                         @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return fileService.findPage(page, size);
    }

    @GetMapping("/search")
    public List<FileDto> search(@RequestParam @NotBlank @Size(max = 300) String query,
                                @RequestParam(defaultValue = "10") int limit,
                                @RequestParam(required = false) Integer year,
                                @RequestParam(required = false) Season season,
                                @RequestParam(required = false) FileType type,
                                @RequestParam(required = false) Long departmentId,
                                @RequestParam(defaultValue = "false") boolean mine,
                                @AuthenticationPrincipal AuthenticatedUser user) {
        var request = new FileSearchRequest(query.trim(), limit, year, season, type, departmentId, mine);
        return searchService.search(request, user);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam FileType type,
                                 @RequestParam Long courseId,
                                 @RequestParam int year,
                                 @RequestParam Season season,
                                 @RequestParam(required = false) @Size(max = 1000) String description,
                                 @AuthenticationPrincipal AuthenticatedUser user) {
        return fileService.upload(file, type, courseId, year, season, description, user);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        FileDownload download = fileService.download(id);
        return ResponseEntity.ok()
                .contentType(download.contentType())
                .contentLength(download.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, download.contentDisposition().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(download.resource());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        fileService.deactivate(id, user);
    }

    @PostMapping("/index")
    public ResponseEntity<IndexJobResponse> index(@RequestParam(defaultValue = "false") boolean all) {
        boolean started = indexingService.startJob(all);
        String message = started
                ? (all ? "Re-indexing all files." : "Indexing files that have no embedding yet.")
                : "An indexing job is already running.";
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new IndexJobResponse(started, message));
    }

    @GetMapping("/index/status")
    public IndexStatus indexStatus() {
        return indexingService.status();
    }
}
