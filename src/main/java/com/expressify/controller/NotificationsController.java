package com.expressify.controller;

import com.expressify.entity.Notification;
import com.expressify.entity.User;
import com.expressify.service.NotificationService;
import com.expressify.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationsController {

    private final UserService userService;
    private final NotificationService notificationService;

    public NotificationsController(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String notifications(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.loadUserEntityByEmail(userDetails.getUsername());
        if (user == null) return "redirect:/auth";
        notificationService.markAllRead(user);
        List<Notification> list = notificationService.getNotifications(user);
        model.addAttribute("currentUser", user);
        model.addAttribute("notifications", list);
        model.addAttribute("unreadCount", 0L);
        model.addAttribute("currentTheme", user.getTheme() != null ? user.getTheme() : "light");
        return "notifications";
    }
}
