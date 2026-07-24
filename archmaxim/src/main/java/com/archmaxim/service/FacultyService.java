package com.archmaxim.service;

import com.archmaxim.entity.Faculty;
import com.archmaxim.repository.FacultyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FacultyService {
    private final FacultyRepository facultyRepository;

    public FacultyService(FacultyRepository facultyRepository) {
        this.facultyRepository = facultyRepository;
    }

    public List<Faculty> getAllFaculties() {
        return facultyRepository.findAll();
    }

    public Faculty createFaculty(String name) {
        return  facultyRepository.save(new Faculty(name));
    }
}
