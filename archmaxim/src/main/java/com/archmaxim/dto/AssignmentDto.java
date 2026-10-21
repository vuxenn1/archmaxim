package com.archmaxim.dto;

import com.archmaxim.entity.CourseLecturer;

public record AssignmentDto(Long id, Long courseId, String courseCode, String courseName,
                            Long lecturerId, String lecturerName) {

    public static AssignmentDto from(CourseLecturer assignment) {
        return new AssignmentDto(assignment.getId(),
                assignment.getCourse().getId(), assignment.getCourse().getCode(), assignment.getCourse().getName(),
                assignment.getLecturer().getId(), assignment.getLecturer().getFullName());
    }
}
