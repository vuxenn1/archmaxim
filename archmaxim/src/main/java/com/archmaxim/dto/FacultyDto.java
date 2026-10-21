package com.archmaxim.dto;

import com.archmaxim.entity.Faculty;

public record FacultyDto(Long id, String name) {

    public static FacultyDto from(Faculty faculty) {
        return new FacultyDto(faculty.getId(), faculty.getName());
    }
}
