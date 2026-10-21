package com.archmaxim.security;

import com.archmaxim.entity.Role;

import java.security.Principal;

/**
 * The caller as described by a verified JWT. {@code id} is the row id in the
 * table that belongs to {@code role}.
 */
public record AuthenticatedUser(long id, String email, Role role) implements Principal {

    @Override
    public String getName() {
        return email;
    }

    public boolean is(Role candidate) {
        return role == candidate;
    }
}
