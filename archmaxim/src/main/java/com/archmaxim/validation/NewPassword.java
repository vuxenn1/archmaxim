package com.archmaxim.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A password that is about to be hashed: at least 8 characters, and no more than
 * the 72 bytes BCrypt can use. Non-ASCII letters such as ğ or ş take two bytes.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NewPasswordValidator.class)
public @interface NewPassword {

    String message() default "must be between 8 and 72 characters long";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
