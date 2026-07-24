package com.archmaxim.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path storageLocation;

    public FileStorageService(@Value("${archmaxim.storage.location}") String location) {
        this.storageLocation = Paths.get(location);
        try {
            Files.createDirectories(storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory: " + location, e);
        }
    }

    public String store(MultipartFile file) {
        String uuid = UUID.randomUUID().toString();
        try {
            Path target = storageLocation.resolve(uuid);
            Files.copy(file.getInputStream(), target);
            return uuid;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public Path load(String uuid) {
        return storageLocation.resolve(uuid);
    }
}