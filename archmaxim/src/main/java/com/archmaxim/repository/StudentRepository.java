package com.archmaxim.repository;

import com.archmaxim.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByStudentNumberIgnoreCase(String studentNumber);

    @Override
    @EntityGraph(attributePaths = "department")
    Page<Student> findAll(Pageable pageable);
}
