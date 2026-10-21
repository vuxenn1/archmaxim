package com.archmaxim.dto;

import com.archmaxim.entity.Student;

public record StudentDto(Long id, String studentNumber, String firstName, String lastName, String email,
                         Long departmentId, String departmentName) {

    public static StudentDto from(Student student) {
        return new StudentDto(student.getId(), student.getStudentNumber(), student.getFirstName(),
                student.getLastName(), student.getEmail(),
                student.getDepartment().getId(), student.getDepartment().getName());
    }
}
