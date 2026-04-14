package com.expressify.service;

import com.expressify.entity.Admin;
import com.expressify.entity.AdminNotification;
import com.expressify.repository.AdminNotificationRepository;
import com.expressify.repository.AdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminNotificationService {

    private final AdminNotificationRepository adminNotificationRepository;
    private final AdminRepository adminRepository;

    public AdminNotificationService(AdminNotificationRepository adminNotificationRepository,
                                    AdminRepository adminRepository) {
        this.adminNotificationRepository = adminNotificationRepository;
        this.adminRepository = adminRepository;
    }

    @Transactional
    public void notifyAdmins(String type, String message, Long postId) {
        List<Admin> admins = adminRepository.findAll();
        for (Admin admin : admins) {
            AdminNotification notification = new AdminNotification();
            notification.setAdmin(admin);
            notification.setType(type);
            notification.setMessage(message);
            notification.setPostId(postId);
            adminNotificationRepository.save(notification);
        }
    }
}

