package com.terrarent.config;

import com.terrarent.entity.Role;
import com.terrarent.entity.User;
import com.terrarent.repository.RoleRepository;
import com.terrarent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.terrarent.entity.Role.RoleName.ROLE_ADMIN;

@Configuration
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // Seed Roles first
        for (Role.RoleName roleName : Role.RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = Role.builder()
                        .name(roleName)
                        .description("Default description for " + roleName)
                        .build();
                roleRepository.save(role);
                System.out.println("✅ Role " + roleName + " created");
            }
        }

        String adminEmail = "admin@terrarent.com";

        if (userRepository.existsByEmail(adminEmail)) {
            return; // admin already exists
        }

        Role adminRole = roleRepository.findByName(ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

        User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode("Admin123!"))
                .role(adminRole)
                .firstName("Admin")    // ✅ Required
                .lastName("User")      // ✅ Required
                .phoneNumber("0000000000") // Optional, if non-nullable
                .status(User.UserStatus.ACTIVE)      // Optional, if required
                .build();


        userRepository.save(admin);

        System.out.println("✅ ADMIN user created");
    }
}
