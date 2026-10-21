package com.archmaxim.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

@Entity
public class Student extends Account {

    private String studentNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Department department;

    protected Student() {
    }

    public Student(String studentNumber, String firstName, String lastName,
                   String email, String passwordHash, Department department) {
        super(email, passwordHash, firstName, lastName);
        this.studentNumber = studentNumber;
        this.department = department;
    }

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public Department getDepartment() {
        return department;
    }
}
