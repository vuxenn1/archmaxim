package com.archmaxim.repository;

import com.archmaxim.entity.Department;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @EntityGraph(attributePaths = "faculty")
    List<Department> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "faculty")
    Optional<Department> findWithFacultyById(Long id);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
