package com.archmaxim.service;

import com.archmaxim.dto.EnrollmentDto;
import com.archmaxim.dto.EnrollmentRequest;
import com.archmaxim.dto.PageResponse;
import com.archmaxim.entity.Course;
import com.archmaxim.entity.Enrollment;
import com.archmaxim.entity.Student;
import com.archmaxim.exception.ConflictException;
import com.archmaxim.exception.NotFoundException;
import com.archmaxim.repository.CourseRepository;
import com.archmaxim.repository.EnrollmentRepository;
import com.archmaxim.repository.StudentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {

    private static final Sort BY_COURSE = Sort.by("course.code", "student.lastName", "id");

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             StudentRepository studentRepository,
                             CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<EnrollmentDto> findPage(int page, int size) {
        return PageResponse.of(enrollmentRepository.findAll(PageRequest.of(page, size, BY_COURSE)), EnrollmentDto::from);
    }

    @Transactional
    public EnrollmentDto enroll(EnrollmentRequest request) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> NotFoundException.of("Student", request.studentId()));
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> NotFoundException.of("Course", request.courseId()));
        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new ConflictException(student.getFullName() + " is already enrolled in " + course.getCode() + ".");
        }
        return EnrollmentDto.from(enrollmentRepository.save(new Enrollment(student, course)));
    }

    @Transactional
    public void delete(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Enrollment", id));
        enrollmentRepository.delete(enrollment);
    }
}
