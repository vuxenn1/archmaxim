package com.archmaxim.controller;

import com.archmaxim.dto.AssignmentDto;
import com.archmaxim.dto.AssignmentRequest;
import com.archmaxim.dto.PageResponse;
import com.archmaxim.service.CourseLecturerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/course-lecturers")
public class CourseLecturerController {

    private final CourseLecturerService assignmentService;

    public CourseLecturerController(CourseLecturerService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public PageResponse<AssignmentDto> getPage(@RequestParam(defaultValue = "0") @Min(0) int page,
                                               @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return assignmentService.findPage(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentDto create(@Valid @RequestBody AssignmentRequest request) {
        return assignmentService.assign(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        assignmentService.delete(id);
    }
}
