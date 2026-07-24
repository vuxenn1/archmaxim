package com.archmaxim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "department", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String name;
    @ManyToOne
    private Faculty faculty;
    private LocalDateTime createdAt;

    public Department() {}

    public Department(String code, String name, Faculty faculty) {
        this.code = code;
        this.name = name;
        this.faculty = faculty;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getCode() {return code;}
    public void setCode(String code) {this.code = code;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    public Faculty getFaculty() {return faculty;}
    public void setFaculty(Faculty faculty) {this.faculty = faculty;}
    public LocalDateTime getCreatedAt() {return createdAt;}
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}
}
