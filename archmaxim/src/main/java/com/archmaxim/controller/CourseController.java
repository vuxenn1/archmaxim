package com.archmaxim.controller;

import com.archmaxim.dto.AliasDto;
import com.archmaxim.dto.AliasRequest;
import com.archmaxim.dto.CourseDto;
import com.archmaxim.dto.CourseRequest;
import com.archmaxim.security.AuthenticatedUser;
import com.archmaxim.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CourseDto> getAll() {
        return courseService.findAll();
    }

    @GetMapping("/mine")
    public List<CourseDto> getMine(@AuthenticationPrincipal AuthenticatedUser user) {
        return courseService.findMine(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseDto create(@Valid @RequestBody CourseRequest request) {
        return courseService.create(request);
    }

    @PutMapping("/{id}")
    public CourseDto update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return courseService.update(id, request);
    }

    @PostMapping("/{courseId}/aliases")
    @ResponseStatus(HttpStatus.CREATED)
    public AliasDto addAlias(@PathVariable Long courseId, @Valid @RequestBody AliasRequest request) {
        return courseService.addAlias(courseId, request);
    }

    @DeleteMapping("/{courseId}/aliases/{aliasId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAlias(@PathVariable Long courseId, @PathVariable Long aliasId) {
        courseService.deleteAlias(courseId, aliasId);
    }
}
