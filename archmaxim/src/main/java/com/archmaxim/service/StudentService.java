package com.archmaxim.service;

import com.archmaxim.dto.PageResponse;
import com.archmaxim.dto.StudentDto;
import com.archmaxim.dto.StudentRequest;
import com.archmaxim.entity.Department;
import com.archmaxim.entity.Student;
import com.archmaxim.exception.ConflictException;
import com.archmaxim.exception.NotFoundException;
import com.archmaxim.repository.DepartmentRepository;
import com.archmaxim.repository.StudentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {

    private static final Sort BY_NAME = Sort.by("lastName", "firstName", "id");

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final AccountDirectory accountDirectory;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository,
                          DepartmentRepository departmentRepository,
                          AccountDirectory accountDirectory,
                          PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.accountDirectory = accountDirectory;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentDto> findPage(int page, int size) {
        return PageResponse.of(studentRepository.findAll(PageRequest.of(page, size, BY_NAME)), StudentDto::from);
    }

    @Transactional
    public StudentDto create(StudentRequest request) {
        String email = AccountDirectory.normalizeEmail(request.email());
        accountDirectory.requireEmailAvailable(email);
        String studentNumber = request.studentNumber().trim();
        if (studentRepository.existsByStudentNumberIgnoreCase(studentNumber)) {
            throw new ConflictException("Student number " + studentNumber + " is already registered.");
        }
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> NotFoundException.of("Department", request.departmentId()));

        Student student = new Student(studentNumber, request.firstName().trim(), request.lastName().trim(),
                email, passwordEncoder.encode(request.password()), department);
        return StudentDto.from(studentRepository.save(student));
    }
}
