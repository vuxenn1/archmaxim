package com.archmaxim.dto;

import com.archmaxim.entity.Course;

import java.util.List;

public record CourseDto(Long id, String code, String name, String description,
                        Long departmentId, String departmentName, List<AliasDto> aliases) {

    public static CourseDto from(Course course) {
        return new CourseDto(course.getId(), course.getCode(), course.getName(), course.getDescription(),
                course.getDepartment().getId(), course.getDepartment().getName(),
                course.getAliases().stream().map(AliasDto::from).toList());
    }
}
