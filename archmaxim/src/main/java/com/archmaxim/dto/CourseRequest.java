package com.archmaxim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank @Size(max = 32) String code,
        @NotBlank @Size(max = 255) String name,
        @NotNull Long departmentId,
        @Size(max = 1000) String description) {
}
