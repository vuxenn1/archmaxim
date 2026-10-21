package com.archmaxim.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Validated
@ConfigurationProperties("archmaxim")
public record ArchmaximProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Embedding embedding,
        @Valid @NotNull Storage storage,
        @Valid @NotNull Admin admin,
        @Valid @NotNull Upload upload,
        @Valid @NotNull Search search,
        @Valid @NotNull Indexing indexing,
        @Valid @NotNull Login login,
        @Valid @DefaultValue Cors cors,
        @Valid @DefaultValue Swagger swagger) {

    public record Jwt(
            @NotBlank(message = "is missing, set ARCHMAXIM_JWT_SECRET") String secret,
            @NotNull Duration expiration) {
    }

    public record Embedding(@NotBlank String url, @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {
    }

    public record Storage(@NotBlank String location) {
    }

    public record Admin(@NotBlank String email, String password) {
    }

    public record Upload(@NotEmpty List<String> allowedExtensions) {

        public Upload {
            allowedExtensions = allowedExtensions == null ? List.of() : allowedExtensions.stream()
                    .map(extension -> extension.trim().toLowerCase(Locale.ROOT))
                    .filter(extension -> !extension.isEmpty())
                    .toList();
        }
    }

    public record Search(
            @Positive double maxDistance,
            @PositiveOrZero double courseBoost,
            @DecimalMin("0.5") @DecimalMax("1.0") double fuzzyThreshold,
            @Min(1) int maxLimit) {
    }

    public record Indexing(@NotNull Duration retryInterval) {
    }

    public record Login(@Min(1) int maxAttempts, @NotNull Duration lockout) {
    }

    public record Cors(List<String> allowedOrigins) {

        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream()
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .toList();
        }
    }

    public record Swagger(boolean enabled) {
    }
}
