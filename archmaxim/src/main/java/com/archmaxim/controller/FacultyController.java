package com.archmaxim.controller;

import com.archmaxim.dto.FacultyDto;
import com.archmaxim.dto.FacultyRequest;
import com.archmaxim.service.FacultyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/faculties")
public class FacultyController {

    private final FacultyService facultyService;

    public FacultyController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @GetMapping
    public List<FacultyDto> getAll() {
        return facultyService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacultyDto create(@Valid @RequestBody FacultyRequest request) {
        return facultyService.create(request);
    }

    @PutMapping("/{id}")
    public FacultyDto update(@PathVariable Long id, @Valid @RequestBody FacultyRequest request) {
        return facultyService.update(id, request);
    }
}
