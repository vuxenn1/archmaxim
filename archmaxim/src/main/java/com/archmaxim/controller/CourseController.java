package com.archmaxim.controller;

import com.archmaxim.entity.Course;
import com.archmaxim.entity.CourseAlias;
import com.archmaxim.service.CourseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<Course> getAll() {
        return courseService.getAllCourses();
    }

    @PostMapping
    public Course create(@RequestBody CourseRequest request) {
        return courseService.createCourse(request.code(), request.name(), request.departmentId(), request.description());
    }

    @PostMapping("/{courseId}/aliases")
    public CourseAlias addAlias(@PathVariable Long courseId, @RequestParam String alias) {
        return courseService.addAlias(courseId, alias);
    }

    public record CourseRequest(String code, String name, Long departmentId, String description) {}
}