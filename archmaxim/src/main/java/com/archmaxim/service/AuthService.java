package com.archmaxim.service;

import com.archmaxim.entity.Lecturer;
import com.archmaxim.entity.Student;
import com.archmaxim.repository.LecturerRepository;
import com.archmaxim.repository.StudentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.archmaxim.entity.Admin;
import com.archmaxim.repository.AdminRepository;

@Service
public class AuthService {

    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;


    public AuthService(LecturerRepository lecturerRepository,
                       StudentRepository studentRepository,
                       AdminRepository adminRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.lecturerRepository = lecturerRepository;
        this.studentRepository = studentRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResult login(String email, String password) {
        // Admin first (highest privilege)
        Admin admin = adminRepository.findByEmail(email).orElse(null);
        if (admin != null && passwordEncoder.matches(password, admin.getPasswordHash())) {
            String name = admin.getFirstName() + " " + admin.getLastName();
            return new LoginResult(jwtService.generateToken(email, "ADMIN"), name, "ADMIN");
        }

        // Then lecturer
        Lecturer lecturer = lecturerRepository.findByEmail(email).orElse(null);
        if (lecturer != null && passwordEncoder.matches(password, lecturer.getPasswordHash())) {
            String name = lecturer.getFirstName() + " " + lecturer.getLastName();
            return new LoginResult(jwtService.generateToken(email, "LECTURER"), name, "LECTURER");
        }

        // Then student
        Student student = studentRepository.findByEmail(email).orElse(null);
        if (student != null && passwordEncoder.matches(password, student.getPasswordHash())) {
            String name = student.getFirstName() + " " + student.getLastName();
            return new LoginResult(jwtService.generateToken(email, "STUDENT"), name, "STUDENT");
        }

        throw new RuntimeException("Invalid email or password");
    }

    public record LoginResult(String token, String name, String role) {}
}