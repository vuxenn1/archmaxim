package com.archmaxim.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NewPasswordValidatorTest {

    @Test
    void acceptsPasswordsBetweenEightCharactersAndSeventyTwoBytes() {
        assertThat(NewPasswordValidator.isAcceptable("ders2026")).isTrue();
        assertThat(NewPasswordValidator.isAcceptable("a".repeat(72))).isTrue();
        assertThat(NewPasswordValidator.isAcceptable("ğ".repeat(36))).isTrue();
    }

    @Test
    void rejectsShortPasswords() {
        assertThat(NewPasswordValidator.isAcceptable("kisa123")).isFalse();
    }

    // BCrypt only reads 72 bytes and Spring Security refuses longer input.
    @Test
    void rejectsPasswordsBCryptCannotHashCompletely() {
        assertThat(NewPasswordValidator.isAcceptable("a".repeat(73))).isFalse();
        assertThat(NewPasswordValidator.isAcceptable("ğ".repeat(37))).isFalse();
    }
}
