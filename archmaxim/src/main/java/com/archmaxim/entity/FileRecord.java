package com.archmaxim.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Metadata of an uploaded file. The content lives on disk under {@link #getUuid()}.
 * The embedding column is read and written with plain SQL since JPA has no vector type.
 */
@Entity
@Table(name = "file_record")
public class FileRecord extends BaseEntity {

    private String uuid;
    private String originalName;

    @Enumerated(EnumType.STRING)
    private FileType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Lecturer uploadedBy;

    private Integer year;

    @Enumerated(EnumType.STRING)
    private Season season;

    private Long sizeBytes;
    private String mimeType;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private int downloadCount;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected FileRecord() {
    }

    public FileRecord(String uuid, String originalName, FileType type, Course course, Lecturer uploadedBy,
                      int year, Season season, long sizeBytes, String mimeType, String description) {
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

    public void deactivate() {
        active = false;
    }

    public String getUuid() {
        return uuid;
    }

    public String getOriginalName() {
        return originalName;
    }

    public FileType getType() {
        return type;
    }

    public Course getCourse() {
        return course;
    }

    public Lecturer getUploadedBy() {
        return uploadedBy;
    }

    public Integer getYear() {
        return year;
    }

    public Season getSeason() {
        return season;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getDescription() {
        return description;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public boolean isActive() {
        return active;
    }
}
