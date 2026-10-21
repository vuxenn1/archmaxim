package com.archmaxim.dto;

import com.archmaxim.entity.Account;
import com.archmaxim.entity.Role;

public record MeResponse(Long id, String email, String name, Role role) {

    public static MeResponse from(Account account) {
        return new MeResponse(account.getId(), account.getEmail(), account.getFullName(), account.getRole());
    }
}
