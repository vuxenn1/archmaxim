package com.archmaxim.config;

import com.archmaxim.entity.Admin;
import com.archmaxim.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(AdminRepository adminRepository,
                      PasswordEncoder passwordEncoder,
                      JdbcTemplate jdbcTemplate) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        // Ensure pgvector is enabled and the embedding column exists,
        // so a fresh database sets itself up with no manual SQL.
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("ALTER TABLE file_record ADD COLUMN IF NOT EXISTS embedding vector(1024)");

        if (adminRepository.findByEmail("admin@archmaxim.edu").isEmpty()) {
            Admin admin = new Admin(
                    "admin@archmaxim.edu",
                    passwordEncoder.encode("admin123"),
                    "System",
                    "Admin"
            );
            adminRepository.save(admin);
            System.out.println(">>> Seeded default admin: admin@archmaxim.edu / admin123");
        }
    }
}