package com.archmaxim.dto;

import com.archmaxim.validation.NewPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 128) String currentPassword,
        @NotBlank @NewPassword String newPassword) {
}
