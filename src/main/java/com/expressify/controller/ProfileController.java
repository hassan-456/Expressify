package com.expressify.controller;

import com.expressify.dto.PostDto;
import com.expressify.entity.User;
import com.expressify.service.FriendService;
import com.expressify.service.NotificationService;
import com.expressify.service.PostService;
import com.expressify.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final PostService postService;
    private final FriendService friendService;
    private final NotificationService notificationService;

    public ProfileController(UserService userService, PostService postService,
            FriendService friendService, NotificationService notificationService) {
        this.userService = userService;
        this.postService = postService;
        this.friendService = friendService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String profile(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "id", required = false) Long id,
            Model model) {
        User currentUser = userService.loadUserEntityByEmail(userDetails.getUsername());
        if (currentUser == null)
            return "redirect:/auth";
        User targetUser = id != null ? userService.findById(id) : currentUser;
        if (targetUser == null)
            return "redirect:/home";
        List<PostDto> posts = postService.getUserPosts(targetUser, currentUser != null ? currentUser.getId() : null);
        int friendCount = friendService.getFriendCount(targetUser);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("profileUser", targetUser);
        model.addAttribute("posts", posts);
        model.addAttribute("friendCount", friendCount);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(currentUser));
        model.addAttribute("currentTheme", currentUser.getTheme() != null ? currentUser.getTheme() : "light");
        return "profile";
    }
}
