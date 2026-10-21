package com.archmaxim.service;

import com.archmaxim.dto.LecturerDto;
import com.archmaxim.dto.LecturerRequest;
import com.archmaxim.dto.PageResponse;
import com.archmaxim.entity.Department;
import com.archmaxim.entity.Lecturer;
import com.archmaxim.exception.NotFoundException;
import com.archmaxim.repository.DepartmentRepository;
import com.archmaxim.repository.LecturerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LecturerService {

    private static final Sort BY_NAME = Sort.by("lastName", "firstName", "id");

    private final LecturerRepository lecturerRepository;
    private final DepartmentRepository departmentRepository;
    private final AccountDirectory accountDirectory;
    private final PasswordEncoder passwordEncoder;

    public LecturerService(LecturerRepository lecturerRepository,
                           DepartmentRepository departmentRepository,
                           AccountDirectory accountDirectory,
                           PasswordEncoder passwordEncoder) {
        this.lecturerRepository = lecturerRepository;
        this.departmentRepository = departmentRepository;
        this.accountDirectory = accountDirectory;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<LecturerDto> findPage(int page, int size) {
        return PageResponse.of(lecturerRepository.findAll(PageRequest.of(page, size, BY_NAME)), LecturerDto::from);
    }

    @Transactional
    public LecturerDto create(LecturerRequest request) {
        String email = AccountDirectory.normalizeEmail(request.email());
        accountDirectory.requireEmailAvailable(email);
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> NotFoundException.of("Department", request.departmentId()));

        Lecturer lecturer = new Lecturer(request.title(), request.firstName().trim(), request.lastName().trim(),
                email, passwordEncoder.encode(request.password()), department);
        return LecturerDto.from(lecturerRepository.save(lecturer));
    }
}
