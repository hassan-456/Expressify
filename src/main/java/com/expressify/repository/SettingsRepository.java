package com.expressify.repository;

import com.expressify.entity.Settings;
import com.expressify.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettingsRepository extends JpaRepository<Settings, Long> {

    Optional<Settings> findByUser(User user);
}
