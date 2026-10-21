package com.archmaxim.entity;

import jakarta.persistence.Entity;

@Entity
public class Faculty extends BaseEntity {

    private String name;

    protected Faculty() {
    }

    public Faculty(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void rename(String name) {
        this.name = name;
    }
}
