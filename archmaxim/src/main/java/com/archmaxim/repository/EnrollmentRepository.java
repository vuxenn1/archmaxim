package com.archmaxim.repository;

import com.archmaxim.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    @Query("select e.course.id from Enrollment e where e.student.id = :studentId")
    List<Long> findCourseIdsByStudentId(@Param("studentId") Long studentId);

    @Override
    @EntityGraph(attributePaths = {"student", "course"})
    Page<Enrollment> findAll(Pageable pageable);
}
