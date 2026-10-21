package com.archmaxim.security;

import com.archmaxim.TestProperties;
import com.archmaxim.entity.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final AuthenticatedUser LECTURER = new AuthenticatedUser(42, "ayse@uni.edu", Role.LECTURER);

    private final JwtService jwtService =
            new JwtService(TestProperties.withSecret(TestProperties.SECRET), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void issuedTokenParsesBackToTheSameUser() {
        IssuedToken token = jwtService.issue(LECTURER);

        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        assertThat(jwtService.parse(token.value())).isEqualTo(LECTURER);
    }

    @Test
    void rejectsSecretsShorterThan32Bytes() {
        assertThatThrownBy(() -> new JwtService(TestProperties.withSecret("too-short"), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void rejectsTamperedTokens() {
        String token = jwtService.issue(LECTURER).value();
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"ayse@uni.edu\",\"role\":\"ADMIN\",\"uid\":42}".getBytes());
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> jwtService.parse(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokensSignedWithAnotherKey() {
        JwtService other = new JwtService(
                TestProperties.withSecret("another-secret-that-is-also-longer-than-32-bytes"),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> jwtService.parse(other.issue(LECTURER).value())).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredTokens() {
        String token = jwtService.issue(LECTURER).value();
        JwtService later = new JwtService(TestProperties.withSecret(TestProperties.SECRET),
                Clock.fixed(NOW.plus(Duration.ofHours(2)), ZoneOffset.UTC));

        assertThatThrownBy(() -> later.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }
}
