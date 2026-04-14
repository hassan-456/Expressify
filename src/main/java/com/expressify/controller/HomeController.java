package com.expressify.controller;

import com.expressify.entity.User;
import com.expressify.service.FriendService;
import com.expressify.service.NotificationService;
import com.expressify.service.PostService;
import com.expressify.service.UserService;
import com.expressify.dto.PostDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final PostService postService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final FriendService friendService;

    public HomeController(PostService postService, UserService userService, NotificationService notificationService,
                          FriendService friendService) {
        this.postService = postService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.friendService = friendService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.loadUserEntityByEmail(userDetails.getUsername());
        if (user == null) return "redirect:/auth";
        model.addAttribute("currentUser", user);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        List<PostDto> posts = postService.getFeedPosts(user.getId(), user);
        model.addAttribute("posts", posts);
        model.addAttribute("currentTheme", user.getTheme() != null ? user.getTheme() : "light");
        model.addAttribute("friendCount", friendService.getFriendCount(user));
        model.addAttribute("postCount", postService.countPostsByUser(user));
        List<FriendService.UserWithStatus> suggestedFriends = friendService.getAllUsersWithStatus(user).stream()
                .filter(uw -> uw.getStatus() == FriendService.FriendshipStatus.NONE)
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("suggestedUsers", suggestedFriends);
        return "home";
    }
}
