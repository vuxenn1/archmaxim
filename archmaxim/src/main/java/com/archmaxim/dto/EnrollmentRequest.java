package com.archmaxim.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(@NotNull Long studentId, @NotNull Long courseId) {
}
