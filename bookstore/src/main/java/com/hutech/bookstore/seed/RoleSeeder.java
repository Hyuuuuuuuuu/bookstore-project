package com.hutech.bookstore.seed;

import com.hutech.bookstore.model.Role;
import com.hutech.bookstore.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Role Seeder - Đảm bảo các role cơ bản luôn tồn tại
 * Chạy trước DataSeeder để đảm bảo roles có sẵn
 * Luôn chạy để đảm bảo roles tồn tại
 */
@Component
@Slf4j
@RequiredArgsConstructor
@Order(0)
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("👑 Ensuring basic roles exist...");

        List<String> roleNames = Arrays.asList("admin", "user", "staff");
        List<String> roleDescriptions = Arrays.asList(
            "Administrator role - Full system access",
            "Regular user role - Basic user access",
            "Staff role - Book management, order management, and customer support"
        );

        for (int i = 0; i < roleNames.size(); i++) {
            String roleName = roleNames.get(i);
            final int index = i;
            roleRepository.findByName(roleName).ifPresentOrElse(
                role -> log.debug("Role '{}' already exists", roleName),
                () -> {
                    Role role = new Role();
                    role.setName(roleName);
                    role.setDescription(roleDescriptions.get(index));
                    role.setIsDeleted(false);
                    roleRepository.save(role);
                    log.info("✅ Created role: {}", roleName);
                }
            );
        }

        log.info("✅ Basic roles ensured");
    }
}

