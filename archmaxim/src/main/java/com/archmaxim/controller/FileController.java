package com.archmaxim.controller;

import com.archmaxim.entity.File;
import com.archmaxim.entity.FileType;
import com.archmaxim.entity.Season;
import com.archmaxim.service.EmbeddingClient;
import com.archmaxim.service.FileService;
import com.archmaxim.service.FileStorageService;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import java.nio.file.Path;

import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;
    private final FileStorageService fileStorageService;
    private final EmbeddingClient embeddingClient;

    public FileController(FileService fileService, FileStorageService fileStorageService, EmbeddingClient embeddingClient) {
        this.fileService = fileService;
        this.fileStorageService = fileStorageService;
        this.embeddingClient = embeddingClient;
    }

    @GetMapping
    public List<File> getAll() {
        return fileService.getAllFiles();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public File upload(@RequestParam("file") MultipartFile file,
                       @RequestParam FileType type,
                       @RequestParam Long courseId,
                       @RequestParam Integer year,
                       @RequestParam Season season,
                       @RequestParam(required = false) String description,
                       Authentication authentication) {
        String email = authentication.getName();
        return fileService.uploadFile(file, type, courseId, email, year, season, description);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        File fileRecord = fileService.getFileForDownload(id);

        Path path = fileStorageService.load(fileRecord.getUuid());
        Resource resource;
        try {
            resource = new UrlResource(path.toUri());
        } catch (Exception e) {
            throw new RuntimeException("File could not be read", e);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileRecord.getOriginalName() + "\"")
                .body(resource);
    }

    @PostMapping("/index")
    public String indexAll() {
        int count = fileService.indexAllFiles();
        return "Indexed " + count + " files";
    }

    @GetMapping("/search")
    public List<File> search(@RequestParam String query,
                             @RequestParam(defaultValue = "5") int limit,
                             @RequestParam(required = false) Integer year,
                             @RequestParam(required = false) Season season,
                             @RequestParam(required = false) FileType type,
                             @RequestParam(required = false) Long departmentId) {
        return fileService.search(query, limit, year, season, type, departmentId);
    }
}