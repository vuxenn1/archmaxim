package com.archmaxim.service;

import com.archmaxim.entity.Department;
import com.archmaxim.entity.Faculty;
import com.archmaxim.repository.DepartmentRepository;
import com.archmaxim.repository.FacultyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final FacultyRepository facultyRepository;

    public  DepartmentService(DepartmentRepository departmentRepository, FacultyRepository facultyRepository) {
        this.departmentRepository = departmentRepository;
        this.facultyRepository = facultyRepository;
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department createDepartment(String code, String name, Long facultyId) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found: " +  facultyId));
        return  departmentRepository.save(new Department(code, name, faculty));
    }
}
