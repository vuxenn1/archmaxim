package com.archmaxim.repository;

import com.archmaxim.entity.CourseLecturer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseLecturerRepository extends JpaRepository<CourseLecturer, Long> {

    boolean existsByCourseIdAndLecturerId(Long courseId, Long lecturerId);

    @Query("select cl.course.id from CourseLecturer cl where cl.lecturer.id = :lecturerId")
    List<Long> findCourseIdsByLecturerId(@Param("lecturerId") Long lecturerId);

    @Override
    @EntityGraph(attributePaths = {"course", "lecturer"})
    Page<CourseLecturer> findAll(Pageable pageable);
}
