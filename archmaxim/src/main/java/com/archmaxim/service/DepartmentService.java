package com.archmaxim.service;

import com.archmaxim.dto.DepartmentDto;
import com.archmaxim.dto.DepartmentRequest;
import com.archmaxim.entity.Department;
import com.archmaxim.entity.Faculty;
import com.archmaxim.exception.ConflictException;
import com.archmaxim.exception.NotFoundException;
import com.archmaxim.repository.CourseRepository;
import com.archmaxim.repository.DepartmentRepository;
import com.archmaxim.repository.FacultyRepository;
import com.archmaxim.search.CourseContentChanged;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final ApplicationEventPublisher events;

    public DepartmentService(DepartmentRepository departmentRepository,
                             FacultyRepository facultyRepository,
                             CourseRepository courseRepository,
                             ApplicationEventPublisher events) {
        this.departmentRepository = departmentRepository;
        this.facultyRepository = facultyRepository;
        this.courseRepository = courseRepository;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> findAll() {
        return departmentRepository.findAllByOrderByNameAsc().stream().map(DepartmentDto::from).toList();
    }

    @Transactional
    public DepartmentDto create(DepartmentRequest request) {
        String code = request.code().trim();
        if (departmentRepository.existsByCodeIgnoreCase(code)) {
            throw duplicate(code);
        }
        Department department = new Department(code, request.name().trim(), requireFaculty(request.facultyId()));
        return DepartmentDto.from(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentDto update(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findWithFacultyById(id)
                .orElseThrow(() -> NotFoundException.of("Department", id));
        String code = request.code().trim();
        if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw duplicate(code);
        }
        department.update(code, request.name().trim(), requireFaculty(request.facultyId()));
        // The department name is part of every file's searchable text.
        events.publishEvent(CourseContentChanged.of(courseRepository.findIdsByDepartmentId(id)));
        return DepartmentDto.from(department);
    }

    private Faculty requireFaculty(Long facultyId) {
        return facultyRepository.findById(facultyId).orElseThrow(() -> NotFoundException.of("Faculty", facultyId));
    }

    private static ConflictException duplicate(String code) {
        return new ConflictException("A department with code " + code + " already exists.");
    }
}
