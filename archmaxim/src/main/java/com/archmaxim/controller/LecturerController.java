package com.archmaxim.controller;

import com.archmaxim.dto.LecturerDto;
import com.archmaxim.dto.LecturerRequest;
import com.archmaxim.dto.PageResponse;
import com.archmaxim.service.LecturerService;
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
@RequestMapping("/lecturers")
public class LecturerController {

    private final LecturerService lecturerService;

    public LecturerController(LecturerService lecturerService) {
        this.lecturerService = lecturerService;
    }

    @GetMapping
    public PageResponse<LecturerDto> getPage(@RequestParam(defaultValue = "0") @Min(0) int page,
                                             @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return lecturerService.findPage(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LecturerDto create(@Valid @RequestBody LecturerRequest request) {
        return lecturerService.create(request);
    }
}
