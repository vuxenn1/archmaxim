package com.archmaxim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FacultyRequest(@NotBlank @Size(max = 255) String name) {
}
