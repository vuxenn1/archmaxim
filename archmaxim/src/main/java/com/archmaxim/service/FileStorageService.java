package com.archmaxim.service;

import com.archmaxim.config.ArchmaximProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Stores uploaded file contents on disk under random names. The original file
 * name only lives in the database, so it never touches the file system.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path root;

    public FileStorageService(ArchmaximProperties properties) {
        this.root = Path.of(properties.storage().location()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create the storage directory " + root, e);
        }
    }

    public String store(MultipartFile file) {
        String name = UUID.randomUUID().toString();
        try (InputStream content = file.getInputStream()) {
            Files.copy(content, resolve(name));
            return name;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store the uploaded file", e);
        }
    }

    public Path resolve(String name) {
        Path path = root.resolve(name).normalize();
        if (!path.getParent().equals(root)) {
            throw new IllegalArgumentException("Invalid storage name: " + name);
        }
        return path;
    }

    public void deleteQuietly(String name) {
        try {
            Files.deleteIfExists(resolve(name));
        } catch (IOException | RuntimeException e) {
            log.warn("Could not delete stored file {}", name, e);
        }
    }
}
