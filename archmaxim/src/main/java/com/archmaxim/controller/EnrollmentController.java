package com.archmaxim.controller;

import com.archmaxim.entity.Enrollment;
import com.archmaxim.service.EnrollmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public List<Enrollment> getAll() {
        return enrollmentService.getAllEnrollments();
    }

    @PostMapping
    public Enrollment create(@RequestBody EnrollmentRequest request) {
        return enrollmentService.enrollStudent(request.studentId(), request.courseId());
    }

    public record EnrollmentRequest(Long studentId, Long courseId) {}
}