package com.archmaxim.service;

import com.archmaxim.entity.Course;
import com.archmaxim.entity.CourseLecturer;
import com.archmaxim.entity.Lecturer;
import com.archmaxim.repository.CourseLecturerRepository;
import com.archmaxim.repository.CourseRepository;
import com.archmaxim.repository.LecturerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseLecturerService {

    private final CourseLecturerRepository courseLecturerRepository;
    private final CourseRepository courseRepository;
    private final LecturerRepository lecturerRepository;

    public CourseLecturerService(CourseLecturerRepository courseLecturerRepository, CourseRepository courseRepository, LecturerRepository lecturerRepository) {
        this.courseLecturerRepository = courseLecturerRepository;
        this.courseRepository = courseRepository;
        this.lecturerRepository = lecturerRepository;
    }

    public List<CourseLecturer> getAllAssignments() {
        return courseLecturerRepository.findAll();
    }

    public CourseLecturer assignLecturerToCourse(Long courseId, Long lecturerId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));
        Lecturer lecturer = lecturerRepository.findById(lecturerId)
                .orElseThrow(() -> new RuntimeException("Lecturer not found: " + lecturerId));
        return courseLecturerRepository.save(new CourseLecturer(course, lecturer));
    }
}