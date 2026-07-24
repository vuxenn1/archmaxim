package com.archmaxim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class CourseAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String alias;

    @ManyToOne
    private Course course;

    private LocalDateTime createdAt;

    public CourseAlias() {}

    public CourseAlias(String alias, Course course) {
        this.alias = alias;
        this.course = course;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}