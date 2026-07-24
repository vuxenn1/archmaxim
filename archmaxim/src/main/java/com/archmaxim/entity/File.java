package com.archmaxim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_record", uniqueConstraints = @UniqueConstraint(columnNames = "uuid"))
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uuid;
    private String originalName;

    @Enumerated(EnumType.STRING)
    private FileType type;

    @ManyToOne
    private Course course;

    @ManyToOne
    private Lecturer uploadedBy;

    private Integer year;

    @Enumerated(EnumType.STRING)
    private Season season;

    private Long sizeBytes;
    private String mimeType;
    private String description;

    private Integer downloadCount = 0;
    private Boolean isActive = true;

    private LocalDateTime createdAt;

    public File() {}

    public File(String uuid, String originalName, FileType type, Course course, Lecturer uploadedBy, Integer year, Season season, Long sizeBytes, String mimeType, String description) {
        this.uuid = uuid;
        this.originalName = originalName;
        this.type = type;
        this.course = course;
        this.uploadedBy = uploadedBy;
        this.year = year;
        this.season = season;
        this.sizeBytes = sizeBytes;
        this.mimeType = mimeType;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public FileType getType() { return type; }
    public void setType(FileType type) { this.type = type; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public Lecturer getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Lecturer uploadedBy) { this.uploadedBy = uploadedBy; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Season getSeason() { return season; }
    public void setSeason(Season season) { this.season = season; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}