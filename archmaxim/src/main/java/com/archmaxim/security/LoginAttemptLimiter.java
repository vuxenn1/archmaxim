package com.archmaxim.security;

import com.archmaxim.config.ArchmaximProperties;
import com.archmaxim.exception.TooManyAttemptsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Locks an email and client address pair after too many failed logins in a row.
 * State is in memory, which is enough for a single backend instance.
 */
@Component
public class LoginAttemptLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptLimiter.class);

    private final int maxAttempts;
    private final Duration lockout;
    private final Clock clock;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public LoginAttemptLimiter(ArchmaximProperties properties, Clock clock) {
        this.maxAttempts = properties.login().maxAttempts();
        this.lockout = properties.login().lockout();
        this.clock = clock;
    }

    public static String key(String email, String clientAddress) {
        return email.trim().toLowerCase(Locale.ROOT) + "|" + clientAddress;
    }

    public void checkAllowed(String key) {
        Attempts current = attempts.get(key);
        Instant now = clock.instant();
        if (current != null && current.isLocked(now)) {
            throw new TooManyAttemptsException(Duration.between(now, current.lockedUntil()));
        }
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();
        Attempts updated = attempts.compute(key, (k, current) -> {
            if (current == null || current.isExpired(now, lockout)) {
                return Attempts.first(now, maxAttempts, lockout);
            }
            return current.next(now, maxAttempts, lockout);
        });
        if (updated.failures() == maxAttempts) {
            log.warn("Login locked for {} after {} failed attempts", key, maxAttempts);
        }
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    void evictExpired() {
        Instant now = clock.instant();
        attempts.values().removeIf(entry -> entry.isExpired(now, lockout));
    }

    private record Attempts(int failures, Instant firstFailure, Instant lockedUntil) {

        static Attempts first(Instant now, int maxAttempts, Duration lockout) {
            return new Attempts(1, now, maxAttempts <= 1 ? now.plus(lockout) : null);
        }

        Attempts next(Instant now, int maxAttempts, Duration lockout) {
            int count = failures + 1;
            return new Attempts(count, firstFailure, count >= maxAttempts ? now.plus(lockout) : null);
        }

        boolean isLocked(Instant now) {
            return lockedUntil != null && now.isBefore(lockedUntil);
        }

        boolean isExpired(Instant now, Duration window) {
            if (lockedUntil != null) {
                return !now.isBefore(lockedUntil);
            }
            return !now.isBefore(firstFailure.plus(window));
        }
    }
}
