package com.archmaxim.service;

import com.archmaxim.entity.Course;
import com.archmaxim.entity.CourseAlias;
import com.archmaxim.entity.Department;
import com.archmaxim.repository.CourseAliasRepository;
import com.archmaxim.repository.CourseRepository;
import com.archmaxim.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {
    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseAliasRepository courseAliasRepository;

    public CourseService(CourseRepository courseRepository,
                         DepartmentRepository departmentRepository,
                         CourseAliasRepository courseAliasRepository) {
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
        this.courseAliasRepository = courseAliasRepository;
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course createCourse(String code, String name, Long departmentId, String description) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found: " + departmentId));
        return courseRepository.save(new Course(code, name, department, description));
    }

    public CourseAlias addAlias(Long courseId, String alias) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));
        return courseAliasRepository.save(new CourseAlias(alias, course));
    }
}