package com.archmaxim.controller;

import com.archmaxim.entity.CourseLecturer;
import com.archmaxim.service.CourseLecturerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/course-lecturers")
public class CourseLecturerController {

    private final CourseLecturerService courseLecturerService;

    public CourseLecturerController(CourseLecturerService courseLecturerService) {
        this.courseLecturerService = courseLecturerService;
    }

    @GetMapping
    public List<CourseLecturer> getAll() {
        return courseLecturerService.getAllAssignments();
    }

    @PostMapping
    public CourseLecturer create(@RequestBody CourseLecturerRequest request) {
        return courseLecturerService.assignLecturerToCourse(request.courseId(), request.lecturerId());
    }

    public record CourseLecturerRequest(Long courseId, Long lecturerId) {}
}