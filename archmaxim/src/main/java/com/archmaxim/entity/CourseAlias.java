package com.archmaxim.entity;

import com.archmaxim.search.TextNormalizer;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

@Entity
public class CourseAlias extends BaseEntity {

    private String alias;

    // Stored so the database enforces uniqueness the same way search compares
    // aliases: case-insensitive and with Turkish characters folded.
    private String normalizedAlias;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Course course;

    protected CourseAlias() {
    }

    public CourseAlias(String alias, Course course) {
        this.alias = alias;
        this.normalizedAlias = TextNormalizer.compact(alias);
        this.course = course;
    }

    public String getAlias() {
        return alias;
    }

    public String getNormalizedAlias() {
        return normalizedAlias;
    }

    public Course getCourse() {
        return course;
    }
}
