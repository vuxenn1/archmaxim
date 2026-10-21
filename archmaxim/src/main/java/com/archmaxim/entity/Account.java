package com.archmaxim.entity;

import jakarta.persistence.MappedSuperclass;

/**
 * Fields shared by the three account tables. Emails must be unique across all
 * of them; the tables are separate, so the service layer enforces that.
 */
@MappedSuperclass
public abstract class Account extends BaseEntity {

    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;

    protected Account() {
    }

    protected Account(String email, String passwordHash, String firstName, String lastName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public abstract Role getRole();

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
