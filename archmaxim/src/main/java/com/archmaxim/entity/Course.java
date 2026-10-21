package com.archmaxim.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
public class Course extends BaseEntity {

    private String code;
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Department department;

    @OneToMany(mappedBy = "course")
    @OrderBy("alias")
    private List<CourseAlias> aliases = new ArrayList<>();

    protected Course() {
    }

    public Course(String code, String name, Department department, String description) {
        update(code, name, department, description);
    }

    public void update(String code, String name, Department department, String description) {
        this.code = code;
        this.name = name;
        this.department = department;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Department getDepartment() {
        return department;
    }

    public List<CourseAlias> getAliases() {
        return Collections.unmodifiableList(aliases);
    }
}
