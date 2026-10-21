package com.archmaxim.controller;

import com.archmaxim.dto.PageResponse;
import com.archmaxim.dto.StudentDto;
import com.archmaxim.dto.StudentRequest;
import com.archmaxim.service.StudentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public PageResponse<StudentDto> getPage(@RequestParam(defaultValue = "0") @Min(0) int page,
                                            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return studentService.findPage(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentDto create(@Valid @RequestBody StudentRequest request) {
        return studentService.create(request);
    }
}
