package com.archmaxim.service;

import com.archmaxim.entity.Department;
import com.archmaxim.entity.Student;
import com.archmaxim.repository.DepartmentRepository;
import com.archmaxim.repository.StudentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;

    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository, DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Student createStudent(String studentNumber, String firstName, String lastName,
                                 String email, String password, Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found: " + departmentId));

        String hashedPassword = passwordEncoder.encode(password);

        return studentRepository.save(
                new Student(studentNumber, firstName, lastName, email, hashedPassword, department));
    }
}