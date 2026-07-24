package com.archmaxim.service;

import com.archmaxim.entity.Department;
import com.archmaxim.entity.Lecturer;
import com.archmaxim.entity.LecturerTitle;
import com.archmaxim.repository.DepartmentRepository;
import com.archmaxim.repository.LecturerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LecturerService {

    private final LecturerRepository lecturerRepository;
    private final DepartmentRepository departmentRepository;

    private final PasswordEncoder passwordEncoder;

    public LecturerService(LecturerRepository lecturerRepository, DepartmentRepository departmentRepository,  PasswordEncoder passwordEncoder) {
        this.lecturerRepository = lecturerRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Lecturer> getAllLecturers() {
        return lecturerRepository.findAll();
    }

    public Lecturer createLecturer(LecturerTitle title, String firstName, String lastName,
                                   String email, String password, Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found: " + departmentId));

        String hashedPassword = passwordEncoder.encode(password);

        return lecturerRepository.save(
                new Lecturer(title, firstName, lastName, email, hashedPassword, department));
    }
}