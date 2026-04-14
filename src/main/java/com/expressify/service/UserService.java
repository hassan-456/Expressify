package com.expressify.service;

import com.expressify.entity.User;
import com.expressify.repository.UserRepository;
import com.expressify.util.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String username, String email, String password) {
        PasswordPolicy.validate(password);
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User loadUserEntityByEmail(String email) {
        return findByEmail(email);
    }

    @Transactional
    public User updateProfile(Long userId, String username, String email, String bio) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setUsername(username);
        user.setEmail(email);
        user.setBio(bio);
        return userRepository.save(user);
    }

    @Transactional
    public void updateProfilePicture(Long userId, String path) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setProfilePicture(path);
        userRepository.save(user);
    }

    @Transactional
    public void updateTheme(Long userId, String theme) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setTheme(theme != null ? theme : "light");
        userRepository.save(user);
    }

    @Transactional
    public void updatePrivacy(Long userId, boolean privateAccount, boolean showActivity) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setPrivateAccount(privateAccount);
        user.setShowActivity(showActivity);
        userRepository.save(user);
    }

    @Transactional
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        PasswordPolicy.validate(newPassword);
        User user = userRepository.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }
}
