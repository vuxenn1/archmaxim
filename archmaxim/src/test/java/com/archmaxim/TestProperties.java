package com.archmaxim;

import com.archmaxim.config.ArchmaximProperties;

import java.time.Duration;
import java.util.List;

public final class TestProperties {

    public static final String SECRET = "test-secret-that-is-comfortably-longer-than-32-bytes";

    private TestProperties() {
    }

    public static ArchmaximProperties withSecret(String secret) {
        return new ArchmaximProperties(
                new ArchmaximProperties.Jwt(secret, Duration.ofHours(1)),
                new ArchmaximProperties.Embedding("http://localhost:8000", Duration.ofSeconds(1), Duration.ofSeconds(5)),
                new ArchmaximProperties.Storage("./storage"),
                new ArchmaximProperties.Admin("admin@archmaxim.edu", null),
                new ArchmaximProperties.Upload(List.of("pdf")),
                new ArchmaximProperties.Search(0.6, 0.1, 0.9, 50),
                new ArchmaximProperties.Indexing(Duration.ofMinutes(5)),
                new ArchmaximProperties.Login(5, Duration.ofMinutes(15)),
                new ArchmaximProperties.Cors(List.of()),
                new ArchmaximProperties.Swagger(false));
    }
}
