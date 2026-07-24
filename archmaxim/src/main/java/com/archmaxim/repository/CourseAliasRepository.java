package com.archmaxim.repository;

import com.archmaxim.entity.CourseAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseAliasRepository extends JpaRepository<CourseAlias, Long> {
    List<CourseAlias> findByAliasIgnoreCase(String alias);
}