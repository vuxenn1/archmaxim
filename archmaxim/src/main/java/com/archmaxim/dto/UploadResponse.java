package com.archmaxim.dto;

/**
 * {@code indexed} is false when the embedding service could not be reached.
 * The file is saved either way and the scheduled retry indexes it later.
 */
public record UploadResponse(FileDto file, boolean indexed) {
}
