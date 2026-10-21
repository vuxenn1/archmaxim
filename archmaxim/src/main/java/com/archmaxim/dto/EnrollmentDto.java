package com.archmaxim.dto;

import com.archmaxim.entity.Enrollment;

public record EnrollmentDto(Long id, Long studentId, String studentNumber, String studentName,
                            Long courseId, String courseCode, String courseName) {

    public static EnrollmentDto from(Enrollment enrollment) {
        return new EnrollmentDto(enrollment.getId(),
                enrollment.getStudent().getId(), enrollment.getStudent().getStudentNumber(),
                enrollment.getStudent().getFullName(),
                enrollment.getCourse().getId(), enrollment.getCourse().getCode(), enrollment.getCourse().getName());
    }
}
