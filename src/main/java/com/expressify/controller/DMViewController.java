package com.expressify.controller;

import com.expressify.entity.User;
import com.expressify.service.NotificationService;
import com.expressify.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DMViewController {

    private final UserService userService;
    private final NotificationService notificationService;

    public DMViewController(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping("/dm")
    public String showDmPage(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            Model model) {
        if (userDetails == null) {
            return "redirect:/auth";
        }

        User currentUser = userService.loadUserEntityByEmail(userDetails.getUsername());
        if (currentUser == null) {
            return "redirect:/auth";
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pageTitle", "Direct Messages - Expressify");
        model.addAttribute("unreadCount", notificationService.getUnreadCount(currentUser));

        String theme = currentUser.getTheme();
        if (theme == null || theme.isEmpty()) {
            theme = "light";
        }
        model.addAttribute("currentTheme", theme);

        return "dm";
    }
}
