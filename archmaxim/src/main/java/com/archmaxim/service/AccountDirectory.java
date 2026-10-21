package com.archmaxim.service;

import com.archmaxim.entity.Account;
import com.archmaxim.exception.ConflictException;
import com.archmaxim.repository.AdminRepository;
import com.archmaxim.repository.LecturerRepository;
import com.archmaxim.repository.StudentRepository;
import com.archmaxim.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Looks up accounts across the admin, lecturer and student tables, and keeps
 * email addresses unique across all three.
 */
@Service
public class AccountDirectory {

    private final AdminRepository adminRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;

    public AccountDirectory(AdminRepository adminRepository,
                            LecturerRepository lecturerRepository,
                            StudentRepository studentRepository) {
        this.adminRepository = adminRepository;
        this.lecturerRepository = lecturerRepository;
        this.studentRepository = studentRepository;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public boolean isEmailInUse(String email) {
        return adminRepository.existsByEmailIgnoreCase(email)
                || lecturerRepository.existsByEmailIgnoreCase(email)
                || studentRepository.existsByEmailIgnoreCase(email);
    }

    public void requireEmailAvailable(String email) {
        if (isEmailInUse(email)) {
            throw new ConflictException("The email " + email + " is already used by another account.");
        }
    }

    /**
     * Every account with this email, highest privilege first. Normally there is at
     * most one, but databases from before the uniqueness check may hold more.
     */
    @Transactional(readOnly = true)
    public List<Account> findByEmail(String email) {
        List<Account> accounts = new ArrayList<>(1);
        adminRepository.findByEmailIgnoreCase(email).ifPresent(accounts::add);
        lecturerRepository.findByEmailIgnoreCase(email).ifPresent(accounts::add);
        studentRepository.findByEmailIgnoreCase(email).ifPresent(accounts::add);
        return accounts;
    }

    @Transactional(readOnly = true)
    public Optional<Account> find(AuthenticatedUser user) {
        return switch (user.role()) {
            case ADMIN -> adminRepository.findById(user.id()).map(Account.class::cast);
            case LECTURER -> lecturerRepository.findById(user.id()).map(Account.class::cast);
            case STUDENT -> studentRepository.findById(user.id()).map(Account.class::cast);
        };
    }
}
