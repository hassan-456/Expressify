package com.expressify.controller;

import com.expressify.entity.User;
import com.expressify.service.PostService;
import com.expressify.service.UserService;
import com.expressify.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final UserService userService;
    private final NotificationService notificationService;

    public PostController(PostService postService, UserService userService, NotificationService notificationService) {
        this.postService = postService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String createPostPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.loadUserEntityByEmail(userDetails.getUsername());
        if (user == null)
            return "redirect:/auth";
        model.addAttribute("currentUser", user);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        model.addAttribute("currentTheme", user.getTheme() != null ? user.getTheme() : "light");
        return "posts";
    }

    @PostMapping
    public String createPost(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "content") String content,
            @RequestParam(name = "media", required = false) MultipartFile media,
            @RequestParam(name = "scheduledTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledTime,
            RedirectAttributes ra) {
        User user = userService.loadUserEntityByEmail(userDetails.getUsername());
        if (user == null)
            return "redirect:/auth";
        try {
            postService.createPost(user, content, media, scheduledTime);
            ra.addFlashAttribute("message", "Post created!");
            return "redirect:/home";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/posts";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to create post. Please try again.");
            return "redirect:/posts";
        }
    }
}
