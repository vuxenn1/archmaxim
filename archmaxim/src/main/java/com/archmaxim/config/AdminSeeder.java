package com.archmaxim.config;

import com.archmaxim.entity.Admin;
import com.archmaxim.repository.AdminRepository;
import com.archmaxim.service.AccountDirectory;
import com.archmaxim.validation.NewPasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first admin on an empty database. Without it nobody could log in
 * to create the other accounts.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);
    private final AdminRepository adminRepository;
    private final AccountDirectory accountDirectory;
    private final PasswordEncoder passwordEncoder;
    private final ArchmaximProperties.Admin settings;

    public AdminSeeder(AdminRepository adminRepository,
                       AccountDirectory accountDirectory,
                       PasswordEncoder passwordEncoder,
                       ArchmaximProperties properties) {
        this.adminRepository = adminRepository;
        this.accountDirectory = accountDirectory;
        this.passwordEncoder = passwordEncoder;
        this.settings = properties.admin();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminRepository.count() > 0) {
            return;
        }

        String password = settings.password();
        if (password == null || !NewPasswordValidator.isAcceptable(password)) {
            throw new IllegalStateException("No admin account exists yet. Set ARCHMAXIM_ADMIN_PASSWORD "
                    + "(" + NewPasswordValidator.MIN_LENGTH + " to " + NewPasswordValidator.MAX_BYTES
                    + " characters) to create the first one.");
        }

        String email = AccountDirectory.normalizeEmail(settings.email());
        if (accountDirectory.isEmailInUse(email)) {
            throw new IllegalStateException("Cannot create the first admin: " + email
                    + " is already used by another account. Set ARCHMAXIM_ADMIN_EMAIL to a different address.");
        }

        adminRepository.save(new Admin(email, passwordEncoder.encode(password), "System", "Admin"));
        log.info("Created the first admin account: {}", email);
    }
}
