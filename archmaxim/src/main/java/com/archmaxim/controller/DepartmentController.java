package com.archmaxim.controller;

import com.archmaxim.entity.Department;
import com.archmaxim.service.DepartmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departments")
public class DepartmentController {
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public List<Department> getALl() {
        return departmentService.getAllDepartments();
    }

    @PostMapping
    public Department create(@RequestBody DepartmentRequest request) {
        return departmentService.createDepartment(request.code(), request.name(),  request.facultyId());
    }

    public record DepartmentRequest(String code, String name, Long facultyId) {}
}
