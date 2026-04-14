package com.expressify.config;

import com.expressify.entity.Admin;
import com.expressify.entity.User;
import com.expressify.repository.AdminRepository;
import com.expressify.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminDataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDataInitializer(AdminRepository adminRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (adminRepository.findByEmail("abdulroot@gmail.com").isEmpty()) {
            Admin admin = new Admin();
            admin.setEmail("abdulroot@gmail.com");
            admin.setPassword(passwordEncoder.encode("1234"));
            admin.setName("Admin User");
            admin.setRole("superadmin");
            adminRepository.save(admin);
            System.out.println("Default admin user created: abdulroot@gmail.com");
        }

        // Initialize MAAS AI Chatbot user
        if (userRepository.findByEmail("maas@expressify.com").isEmpty()) {
            User maasUser = new User();
            maasUser.setUsername("MAAS AI");
            maasUser.setEmail("maas@expressify.com");
            maasUser.setPassword(passwordEncoder.encode("maas_secure_password"));
            maasUser.setProfilePicture("https://api.dicebear.com/7.x/bottts/svg?seed=MAAS&backgroundColor=4f46e5");
            maasUser.setStatus(User.UserStatus.active);
            userRepository.save(maasUser);
            System.out.println("MAAS AI Chatbot user created: maas@expressify.com");
        }
    }
}
