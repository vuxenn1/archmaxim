package com.archmaxim.dto;

import jakarta.validation.constraints.NotNull;

public record AssignmentRequest(@NotNull Long courseId, @NotNull Long lecturerId) {
}
