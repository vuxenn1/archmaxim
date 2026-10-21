package com.archmaxim.entity;

import jakarta.persistence.Entity;

@Entity
public class Admin extends Account {

    protected Admin() {
    }

    public Admin(String email, String passwordHash, String firstName, String lastName) {
        super(email, passwordHash, firstName, lastName);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}
