package com.archmaxim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AliasRequest(@NotBlank @Size(max = 64) String alias) {
}
