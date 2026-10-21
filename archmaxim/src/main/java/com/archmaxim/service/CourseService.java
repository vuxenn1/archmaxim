package com.archmaxim.service;

import com.archmaxim.dto.AliasDto;
import com.archmaxim.dto.AliasRequest;
import com.archmaxim.dto.CourseDto;
import com.archmaxim.dto.CourseRequest;
import com.archmaxim.entity.Course;
import com.archmaxim.entity.CourseAlias;
import com.archmaxim.entity.Department;
import com.archmaxim.exception.BadRequestException;
import com.archmaxim.exception.ConflictException;
import com.archmaxim.exception.ForbiddenException;
import com.archmaxim.exception.NotFoundException;
import com.archmaxim.repository.CourseAliasRepository;
import com.archmaxim.repository.CourseLecturerRepository;
import com.archmaxim.repository.CourseRepository;
import com.archmaxim.repository.DepartmentRepository;
import com.archmaxim.repository.EnrollmentRepository;
import com.archmaxim.search.CourseContentChanged;
import com.archmaxim.search.TextNormalizer;
import com.archmaxim.security.AuthenticatedUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseAliasRepository aliasRepository;
    private final CourseLecturerRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher events;

    public CourseService(CourseRepository courseRepository,
                         DepartmentRepository departmentRepository,
                         CourseAliasRepository aliasRepository,
                         CourseLecturerRepository assignmentRepository,
                         EnrollmentRepository enrollmentRepository,
                         ApplicationEventPublisher events) {
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
        this.aliasRepository = aliasRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<CourseDto> findAll() {
        return courseRepository.findAllByOrderByCodeAsc().stream().map(CourseDto::from).toList();
    }

    /**
     * Courses a lecturer teaches or a student is enrolled in.
     */
    @Transactional(readOnly = true)
    public List<CourseDto> findMine(AuthenticatedUser user) {
        List<Long> ids = switch (user.role()) {
            case LECTURER -> assignmentRepository.findCourseIdsByLecturerId(user.id());
            case STUDENT -> enrollmentRepository.findCourseIdsByStudentId(user.id());
            case ADMIN -> throw new ForbiddenException("Admins are not linked to courses.");
        };
        if (ids.isEmpty()) {
            return List.of();
        }
        return courseRepository.findByIdInOrderByCodeAsc(ids).stream().map(CourseDto::from).toList();
    }

    @Transactional
    public CourseDto create(CourseRequest request) {
        Department department = requireDepartment(request.departmentId());
        String code = request.code().trim();
        if (courseRepository.existsByDepartmentIdAndCodeIgnoreCase(department.getId(), code)) {
            throw duplicate(code, department);
        }
        Course course = courseRepository.save(
                new Course(code, request.name().trim(), department, blankToNull(request.description())));
        // A new course has no files yet, so only the catalog needs refreshing.
        events.publishEvent(new CourseContentChanged(List.of()));
        return CourseDto.from(course);
    }

    @Transactional
    public CourseDto update(Long id, CourseRequest request) {
        Course course = courseRepository.findWithDetailsById(id).orElseThrow(() -> NotFoundException.of("Course", id));
        Department department = requireDepartment(request.departmentId());
        String code = request.code().trim();
        if (courseRepository.existsByDepartmentIdAndCodeIgnoreCaseAndIdNot(department.getId(), code, id)) {
            throw duplicate(code, department);
        }
        course.update(code, request.name().trim(), department, blankToNull(request.description()));
        events.publishEvent(CourseContentChanged.of(id));
        return CourseDto.from(course);
    }

    @Transactional
    public AliasDto addAlias(Long courseId, AliasRequest request) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> NotFoundException.of("Course", courseId));
        String alias = request.alias().trim();
        String normalized = TextNormalizer.compact(alias);
        if (normalized.isEmpty()) {
            throw new BadRequestException("An alias needs at least one letter or digit.");
        }
        if (aliasRepository.existsByNormalizedAlias(normalized)) {
            throw new ConflictException("The alias " + alias + " is already in use.");
        }
        CourseAlias saved = aliasRepository.save(new CourseAlias(alias, course));
        events.publishEvent(CourseContentChanged.of(courseId));
        return AliasDto.from(saved);
    }

    @Transactional
    public void deleteAlias(Long courseId, Long aliasId) {
        CourseAlias alias = aliasRepository.findByIdAndCourseId(aliasId, courseId)
                .orElseThrow(() -> NotFoundException.of("Alias", aliasId));
        aliasRepository.delete(alias);
        events.publishEvent(CourseContentChanged.of(courseId));
    }

    private Department requireDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> NotFoundException.of("Department", departmentId));
    }

    private static ConflictException duplicate(String code, Department department) {
        return new ConflictException("Course " + code + " already exists in " + department.getName() + ".");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
