package io.github.milyor.doc_storage_api.config;

import io.github.milyor.doc_storage_api.model.Users;
import io.github.milyor.doc_storage_api.repository.UserRep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.logging.Logger;

/**
 * Seeds a default user on startup if one does not already exist.
 * Idempotent: safe to run on every boot, locally and on the deployed instance.
 * Credentials come from env vars so no plaintext password lives in the repo.
 */

@Configuration
public class DataSeeder {

    private static final Logger log = Logger.getLogger(DataSeeder.class.getName());

    @Bean
    public CommandLineRunner seedDefaultUser(
            UserRep userRep,
            PasswordEncoder passwordEncoder,
            @Value("${SEED_USER_NAME:admin}") String username,
            @Value("${SEED_USER_PASSWORD:changeme}") String rawPassword,
            @Value("${SEED_USER_ROLE:ROLE_ADMIN}") String role) {

        return args -> {
            if (userRep.findByUsername(username) != null) {
                log.info("Seed user '" + username + "' already exists; skipping.");
                return;
            }
            Users user = new Users();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            userRep.save(user);
            log.info("Seeded default user '" + username + "' with role " + role + ".");
        };
    }
}
