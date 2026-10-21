package com.archmaxim.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class NewPasswordValidator implements ConstraintValidator<NewPassword, String> {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Null and blank values are left to @NotBlank.
        return password == null || isAcceptable(password);
    }

    public static boolean isAcceptable(String password) {
        return password.length() >= MIN_LENGTH
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }
}
