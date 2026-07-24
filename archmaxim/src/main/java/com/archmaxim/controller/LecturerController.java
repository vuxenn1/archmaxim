package com.archmaxim.controller;

import com.archmaxim.entity.Lecturer;
import com.archmaxim.entity.LecturerTitle;
import com.archmaxim.service.LecturerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lecturers")
public class LecturerController {

    private final LecturerService lecturerService;

    public LecturerController(LecturerService lecturerService) {
        this.lecturerService = lecturerService;
    }

    @GetMapping
    public List<Lecturer> getAll() {
        return lecturerService.getAllLecturers();
    }

    @PostMapping
    public Lecturer create(@RequestBody LecturerRequest request) {
        return lecturerService.createLecturer(
                request.title(), request.firstName(), request.lastName(),
                request.email(), request.passwordHash(), request.departmentId());
    }

    public record LecturerRequest(LecturerTitle title, String firstName, String lastName, String email, String passwordHash, Long departmentId) {}
}