package com.archmaxim.service;

import com.archmaxim.dto.FacultyDto;
import com.archmaxim.dto.FacultyRequest;
import com.archmaxim.entity.Faculty;
import com.archmaxim.exception.ConflictException;
import com.archmaxim.exception.NotFoundException;
import com.archmaxim.repository.FacultyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FacultyService {

    private final FacultyRepository facultyRepository;

    public FacultyService(FacultyRepository facultyRepository) {
        this.facultyRepository = facultyRepository;
    }

    @Transactional(readOnly = true)
    public List<FacultyDto> findAll() {
        return facultyRepository.findAllByOrderByNameAsc().stream().map(FacultyDto::from).toList();
    }

    @Transactional
    public FacultyDto create(FacultyRequest request) {
        String name = request.name().trim();
        if (facultyRepository.existsByNameIgnoreCase(name)) {
            throw duplicate(name);
        }
        return FacultyDto.from(facultyRepository.save(new Faculty(name)));
    }

    @Transactional
    public FacultyDto update(Long id, FacultyRequest request) {
        Faculty faculty = facultyRepository.findById(id).orElseThrow(() -> NotFoundException.of("Faculty", id));
        String name = request.name().trim();
        if (facultyRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicate(name);
        }
        faculty.rename(name);
        return FacultyDto.from(faculty);
    }

    private static ConflictException duplicate(String name) {
        return new ConflictException("A faculty named " + name + " already exists.");
    }
}
