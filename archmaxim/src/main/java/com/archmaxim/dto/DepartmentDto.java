package com.archmaxim.dto;

import com.archmaxim.entity.Department;

public record DepartmentDto(Long id, String code, String name, Long facultyId, String facultyName) {

    public static DepartmentDto from(Department department) {
        return new DepartmentDto(department.getId(), department.getCode(), department.getName(),
                department.getFaculty().getId(), department.getFaculty().getName());
    }
}
