package com.archmaxim.security;

import com.archmaxim.config.ArchmaximProperties;
import com.archmaxim.entity.Account;
import com.archmaxim.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    // HS256 needs a key of at least 256 bits.
    private static final int MIN_SECRET_BYTES = 32;

    private static final String ROLE_CLAIM = "role";
    private static final String USER_ID_CLAIM = "uid";

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;
    private final JwtParser parser;

    public JwtService(ArchmaximProperties properties, Clock clock) {
        byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("ARCHMAXIM_JWT_SECRET must be at least " + MIN_SECRET_BYTES
                    + " bytes long, it is " + secret.length + ".");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.expiration = properties.jwt().expiration();
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    public IssuedToken issue(Account account) {
        return issue(new AuthenticatedUser(account.getId(), account.getEmail(), account.getRole()));
    }

    public IssuedToken issue(AuthenticatedUser user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(expiration);
        String token = Jwts.builder()
                .subject(user.email())
                .claim(ROLE_CLAIM, user.role().name())
                .claim(USER_ID_CLAIM, user.id())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /**
     * Verifies the signature and expiry and reads the caller out of the token.
     *
     * @throws JwtException if the token is invalid, expired or missing a claim
     */
    public AuthenticatedUser parse(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();

        String role = claims.get(ROLE_CLAIM, String.class);
        Number userId = claims.get(USER_ID_CLAIM, Number.class);
        if (role == null || userId == null || claims.getSubject() == null) {
            throw new JwtException("Token is missing required claims");
        }
        try {
            return new AuthenticatedUser(userId.longValue(), claims.getSubject(), Role.valueOf(role));
        } catch (IllegalArgumentException e) {
            throw new JwtException("Unknown role in token: " + role);
        }
    }
}
