package com.archmaxim.security;

import com.archmaxim.TestProperties;
import com.archmaxim.exception.TooManyAttemptsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-03-01T10:00:00Z"));
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(TestProperties.withSecret(TestProperties.SECRET), clock);
    private final String key = LoginAttemptLimiter.key(" Ayse@Uni.edu ", "10.0.0.1");

    @Test
    void normalizesTheEmailInTheKey() {
        assertThat(key).isEqualTo("ayse@uni.edu|10.0.0.1");
    }

    @Test
    void locksAfterTheConfiguredNumberOfFailures() {
        for (int i = 0; i < 5; i++) {
            limiter.checkAllowed(key);
            limiter.recordFailure(key);
        }

        assertThatThrownBy(() -> limiter.checkAllowed(key))
                .isInstanceOfSatisfying(TooManyAttemptsException.class,
                        e -> assertThat(e.retryAfterSeconds()).isEqualTo(Duration.ofMinutes(15).toSeconds()));
    }

    @Test
    void unlocksWhenTheLockoutIsOver() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(key);
        }
        clock.advance(Duration.ofMinutes(15));

        assertThatCode(() -> limiter.checkAllowed(key)).doesNotThrowAnyException();
    }

    @Test
    void successClearsEarlierFailures() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(key);
        }
        limiter.recordSuccess(key);
        limiter.recordFailure(key);

        assertThatCode(() -> limiter.checkAllowed(key)).doesNotThrowAnyException();
    }

    @Test
    void otherAddressesAreNotAffected() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(key);
        }

        assertThatCode(() -> limiter.checkAllowed(LoginAttemptLimiter.key("ayse@uni.edu", "10.0.0.2")))
                .doesNotThrowAnyException();
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
