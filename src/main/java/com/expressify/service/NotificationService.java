package com.expressify.service;

import com.expressify.entity.Notification;
import com.expressify.entity.User;
import com.expressify.repository.NotificationRepository;
import com.expressify.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createNotification(Long userId, String type, String message, Long relatedUserId, Long postId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setMessage(message);
        n.setRelatedUserId(relatedUserId);
        n.setPostId(postId);
        notificationRepository.save(n);
    }

    public void createNotification(User user, String type, String message, Long relatedUserId, Long postId) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setMessage(message);
        n.setRelatedUserId(relatedUserId);
        n.setPostId(postId);
        notificationRepository.save(n);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsRead(user, false);
    }

    public List<Notification> getNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, 50));
    }

    @Transactional
    public void markAllRead(User user) {
        List<Notification> list = notificationRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, 500));
        list.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(list);
    }
}
