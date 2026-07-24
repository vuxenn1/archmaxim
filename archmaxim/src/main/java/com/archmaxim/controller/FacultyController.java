package com.archmaxim.controller;

import com.archmaxim.entity.Faculty;
import com.archmaxim.service.FacultyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/faculties")
public class FacultyController {
    private final FacultyService facultyService;

    public FacultyController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @GetMapping
    public List<Faculty> getAll() {
        return facultyService.getAllFaculties();
    }

    @PostMapping
    public Faculty createFaculty(@RequestBody FacultyRequest request) {
        return facultyService.createFaculty(request.name());
    }

    public record FacultyRequest(String name) {}
}