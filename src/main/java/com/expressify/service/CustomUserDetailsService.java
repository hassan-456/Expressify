package com.expressify.service;

import com.expressify.entity.User;
import com.expressify.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        boolean isBanned = (user.getStatus() == User.UserStatus.banned);

        // Return UserDetails with enabled=false for banned users.
        // Spring Security's AbstractUserDetailsAuthenticationProvider will
        // check isEnabled() and throw DisabledException automatically,
        // which our custom failureHandler in SecurityConfig can then detect.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                !isBanned, // enabled (false if banned)
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public User loadUserEntityByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}
