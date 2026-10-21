package com.archmaxim.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

@Entity
public class Department extends BaseEntity {

    private String code;
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Faculty faculty;

    protected Department() {
    }

    public Department(String code, String name, Faculty faculty) {
        update(code, name, faculty);
    }

    public void update(String code, String name, Faculty faculty) {
        this.code = code;
        this.name = name;
        this.faculty = faculty;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Faculty getFaculty() {
        return faculty;
    }
}
