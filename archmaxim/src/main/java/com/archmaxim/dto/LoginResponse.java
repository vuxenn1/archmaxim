package com.archmaxim.dto;

import com.archmaxim.entity.Role;

import java.time.Instant;

public record LoginResponse(String token, Long id, String name, Role role, Instant expiresAt) {
}
