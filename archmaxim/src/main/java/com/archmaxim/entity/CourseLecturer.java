package com.archmaxim.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

@Entity
public class CourseLecturer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Lecturer lecturer;

    protected CourseLecturer() {
    }

    public CourseLecturer(Course course, Lecturer lecturer) {
        this.course = course;
        this.lecturer = lecturer;
    }

    public Course getCourse() {
        return course;
    }

    public Lecturer getLecturer() {
        return lecturer;
    }
}
