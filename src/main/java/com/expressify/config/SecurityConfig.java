package com.expressify.config;

import com.expressify.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final CustomUserDetailsService userDetailsService;

        public SecurityConfig(CustomUserDetailsService userDetailsService) {
                this.userDetailsService = userDetailsService;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http,
                        com.expressify.security.MaasChatCleanupLogoutHandler maasChatCleanupLogoutHandler)
                        throws Exception {
                http
                                .csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/api/**"),
                                                new AntPathRequestMatcher("/admin/**")))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/auth", "/auth/**", "/login", "/register",
                                                                "/css/**", "/js/**", "/admin/css/**", "/assets/**",
                                                                "/images/**", "/uploads/**", "/themes_*.css", "/error")
                                                .permitAll()
                                                .requestMatchers("/admin/**").permitAll()
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/auth")
                                                .loginProcessingUrl("/auth/login")
                                                .defaultSuccessUrl("/home", true)
                                                .failureHandler((request, response, exception) -> {
                                                        if (exception instanceof org.springframework.security.authentication.DisabledException) {
                                                                response.sendRedirect("/auth?banned=true");
                                                        } else {
                                                                response.sendRedirect("/auth?error=true");
                                                        }
                                                })
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .addLogoutHandler(maasChatCleanupLogoutHandler)
                                                .logoutSuccessUrl("/")
                                                .invalidateHttpSession(true)
                                                .permitAll())
                                .userDetailsService(userDetailsService);
                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
