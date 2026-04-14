package com.expressify.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.expressify.entity.User;
import com.expressify.service.FriendService;
import com.expressify.service.NotificationService;
import com.expressify.service.UserService;

@Controller
@RequestMapping("/friends")
public class FriendsController {

    private final UserService userService;
    private final FriendService friendService;
    private final NotificationService notificationService;

    public FriendsController(UserService userService, FriendService friendService, NotificationService notificationService) {
        this.userService = userService;
        this.friendService = friendService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String friends(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "q", required = false) String query,
            Model model) {

        User user = userService.loadUserEntityByEmail(userDetails.getUsername());
        if (user == null) return "redirect:/auth";

        List<FriendService.UserWithStatus> users = friendService.getAllUsersWithStatus(user);

        // ✅ SAFE SEARCH (no new service needed)
        if (query != null && !query.trim().isEmpty()) {
            users = users.stream()
                    .filter(u -> u.getUser().getUsername().toLowerCase()
                            .contains(query.toLowerCase()))
                    .toList();

            model.addAttribute("searchQuery", query);
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("users", users);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        model.addAttribute("currentTheme", user.getTheme() != null ? user.getTheme() : "light");

        return "friends";
    }
}
