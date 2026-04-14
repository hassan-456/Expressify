package com.expressify.repository;

import com.expressify.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findTop5ByOrderByCreatedAtDesc();

    long countByCreatedAtAfter(java.time.Instant date);
}
