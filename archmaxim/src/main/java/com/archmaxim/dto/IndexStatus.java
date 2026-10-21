package com.archmaxim.dto;

import java.time.Instant;

public record IndexStatus(boolean running, int total, int processed, int failed,
                          Instant startedAt, Instant finishedAt) {

    public static IndexStatus idle() {
        return new IndexStatus(false, 0, 0, 0, null, null);
    }
}
