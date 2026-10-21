package com.archmaxim.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

@Entity
public class Lecturer extends Account {

    @Enumerated(EnumType.STRING)
    private LecturerTitle title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Department department;

    protected Lecturer() {
    }

    public Lecturer(LecturerTitle title, String firstName, String lastName,
                    String email, String passwordHash, Department department) {
        super(email, passwordHash, firstName, lastName);
        this.title = title;
        this.department = department;
    }

    @Override
    public Role getRole() {
        return Role.LECTURER;
    }

    public LecturerTitle getTitle() {
        return title;
    }

    public Department getDepartment() {
        return department;
    }
}
