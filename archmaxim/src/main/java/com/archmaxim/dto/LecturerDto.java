package com.archmaxim.dto;

import com.archmaxim.entity.Lecturer;
import com.archmaxim.entity.LecturerTitle;

public record LecturerDto(Long id, LecturerTitle title, String firstName, String lastName, String email,
                          Long departmentId, String departmentName) {

    public static LecturerDto from(Lecturer lecturer) {
        return new LecturerDto(lecturer.getId(), lecturer.getTitle(), lecturer.getFirstName(),
                lecturer.getLastName(), lecturer.getEmail(),
                lecturer.getDepartment().getId(), lecturer.getDepartment().getName());
    }
}
