package com.archmaxim.repository;

import com.archmaxim.entity.Course;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @EntityGraph(attributePaths = {"department", "aliases"})
    List<Course> findAllByOrderByCodeAsc();

    @EntityGraph(attributePaths = {"department", "aliases"})
    List<Course> findByIdInOrderByCodeAsc(Collection<Long> ids);

    @EntityGraph(attributePaths = {"department", "aliases"})
    Optional<Course> findWithDetailsById(Long id);

    boolean existsByDepartmentIdAndCodeIgnoreCase(Long departmentId, String code);

    boolean existsByDepartmentIdAndCodeIgnoreCaseAndIdNot(Long departmentId, String code, Long id);

    @Query("select c.id from Course c where c.department.id = :departmentId")
    List<Long> findIdsByDepartmentId(@Param("departmentId") Long departmentId);
}
