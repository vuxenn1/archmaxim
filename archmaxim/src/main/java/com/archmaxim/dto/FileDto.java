package com.archmaxim.dto;

import com.archmaxim.entity.Course;
import com.archmaxim.entity.Department;
import com.archmaxim.entity.FileRecord;
import com.archmaxim.entity.FileType;
import com.archmaxim.entity.Lecturer;
import com.archmaxim.entity.LecturerTitle;
import com.archmaxim.entity.Season;

import java.time.LocalDateTime;

/**
 * Public view of an uploaded file. Leaves out the storage name, the active flag
 * and the uploader's email on purpose.
 */
public record FileDto(Long id, String originalName, FileType type, Integer year, Season season,
                      Long sizeBytes, String mimeType, String description, int downloadCount,
                      LocalDateTime createdAt, Double relevance,
                      CourseRef course, DepartmentRef department, UploaderRef uploadedBy) {

    public record CourseRef(Long id, String code, String name) {
    }

    public record DepartmentRef(Long id, String code, String name) {
    }

    public record UploaderRef(Long id, String name, LecturerTitle title) {
    }

    public static FileDto from(FileRecord file) {
        return from(file, null);
    }

    public static FileDto from(FileRecord file, Double relevance) {
        Course course = file.getCourse();
        Department department = course.getDepartment();
        Lecturer uploader = file.getUploadedBy();
        return new FileDto(file.getId(), file.getOriginalName(), file.getType(), file.getYear(), file.getSeason(),
                file.getSizeBytes(), file.getMimeType(), file.getDescription(), file.getDownloadCount(),
                file.getCreatedAt(), relevance,
                new CourseRef(course.getId(), course.getCode(), course.getName()),
                new DepartmentRef(department.getId(), department.getCode(), department.getName()),
                new UploaderRef(uploader.getId(), uploader.getFullName(), uploader.getTitle()));
    }
}
