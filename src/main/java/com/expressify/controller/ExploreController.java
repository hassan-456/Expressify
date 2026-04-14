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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/explore")
public class ExploreController {

    private final PostService postService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final FriendService friendService;

    public ExploreController(PostService postService, UserService userService,
                             NotificationService notificationService, FriendService friendService) {
        this.postService = postService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.friendService = friendService;
    }

    @GetMapping
    public String explore(@AuthenticationPrincipal UserDetails userDetails,
                          @RequestParam(name = "q", required = false) String query,
                          Model model) {
        User user = userService.loadUserEntityByEmail(userDetails.getUsername());
        if (user == null) return "redirect:/auth";
        List<PostDto> feed = postService.getFeedPosts(user.getId(), user);
        Collections.shuffle(feed);
        List<PostDto> randomPosts = feed.stream().limit(10).collect(Collectors.toList());
        model.addAttribute("currentUser", user);
        model.addAttribute("posts", randomPosts);
        if (query != null && !query.trim().isEmpty()) {
            model.addAttribute("searchQuery", query);
            model.addAttribute("userResults", friendService.searchUsersWithStatus(user, query));
        }
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        model.addAttribute("currentTheme", user.getTheme() != null ? user.getTheme() : "light");
        return "explore";
    }
}
