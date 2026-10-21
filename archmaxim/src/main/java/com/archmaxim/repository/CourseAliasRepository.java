package com.archmaxim.repository;

import com.archmaxim.entity.CourseAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseAliasRepository extends JpaRepository<CourseAlias, Long> {

    boolean existsByNormalizedAlias(String normalizedAlias);

    Optional<CourseAlias> findByIdAndCourseId(Long id, Long courseId);
}
