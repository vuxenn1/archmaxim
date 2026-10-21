package com.archmaxim.service;

import com.archmaxim.dto.AssignmentDto;
import com.archmaxim.dto.AssignmentRequest;
import com.archmaxim.dto.PageResponse;
import com.archmaxim.entity.Course;
import com.archmaxim.entity.CourseLecturer;
import com.archmaxim.entity.Lecturer;
import com.archmaxim.exception.ConflictException;
import com.archmaxim.exception.NotFoundException;
import com.archmaxim.repository.CourseLecturerRepository;
import com.archmaxim.repository.CourseRepository;
import com.archmaxim.repository.LecturerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Which lecturers teach which courses. A lecturer can only upload files to
 * courses they are assigned to.
 */
@Service
public class CourseLecturerService {

    private static final Sort BY_COURSE = Sort.by("course.code", "lecturer.lastName", "id");

    private final CourseLecturerRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final LecturerRepository lecturerRepository;

    public CourseLecturerService(CourseLecturerRepository assignmentRepository,
                                 CourseRepository courseRepository,
                                 LecturerRepository lecturerRepository) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
        this.lecturerRepository = lecturerRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AssignmentDto> findPage(int page, int size) {
        return PageResponse.of(assignmentRepository.findAll(PageRequest.of(page, size, BY_COURSE)), AssignmentDto::from);
    }

    @Transactional
    public AssignmentDto assign(AssignmentRequest request) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> NotFoundException.of("Course", request.courseId()));
        Lecturer lecturer = lecturerRepository.findById(request.lecturerId())
                .orElseThrow(() -> NotFoundException.of("Lecturer", request.lecturerId()));
        if (assignmentRepository.existsByCourseIdAndLecturerId(course.getId(), lecturer.getId())) {
            throw new ConflictException(lecturer.getFullName() + " is already assigned to " + course.getCode() + ".");
        }
        return AssignmentDto.from(assignmentRepository.save(new CourseLecturer(course, lecturer)));
    }

    @Transactional
    public void delete(Long id) {
        CourseLecturer assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Assignment", id));
        assignmentRepository.delete(assignment);
    }
}
