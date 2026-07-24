package com.archmaxim.controller;

import com.archmaxim.entity.Student;
import com.archmaxim.service.StudentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public List<Student> getAll() {
        return studentService.getAllStudents();
    }

    @PostMapping
    public Student create(@RequestBody StudentRequest request) {
        return studentService.createStudent(
                request.studentNumber(), request.firstName(), request.lastName(),
                request.email(), request.passwordHash(), request.departmentId());
    }

    public record StudentRequest(String studentNumber, String firstName, String lastName, String email, String passwordHash, Long departmentId) {}
}