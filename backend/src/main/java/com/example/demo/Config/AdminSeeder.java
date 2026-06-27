package com.example.demo.Config;

import com.example.demo.Enums.UserRole;
import com.example.demo.Model.User;
import com.example.demo.Repository.IUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Na startu aplikacije napravi podrazumevani ADMIN nalog ako još ne postoji.
 * Idempotentno je: ako admin već postoji (po email-u), ne radi ništa.
 * Tako svaka sveža baza (npr. nov docker volume) odmah ima nalog za prijavu,
 * bez ručnog ubacivanja u bazu.
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminSeeder.class);

    private static final String ADMIN_EMAIL = "admin@admin.com";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String ADMIN_NAME = "Administrator";

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(IUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }

        User admin = new User();
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setName(ADMIN_NAME);
        admin.setRole(UserRole.ADMIN);
        // createdAt je već postavljen na LocalDateTime.now() u entitetu

        userRepository.save(admin);
        logger.warn("Default ADMIN kreiran -> email='{}', lozinka='{}'", ADMIN_EMAIL, ADMIN_PASSWORD);
    }
}
