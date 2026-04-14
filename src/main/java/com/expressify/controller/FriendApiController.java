package com.expressify.controller;

import com.expressify.entity.User;
import com.expressify.service.FriendService;
import com.expressify.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class FriendApiController {

    private final FriendService friendService;
    private final UserService userService;

    public FriendApiController(FriendService friendService, UserService userService) {
        this.friendService = friendService;
        this.userService = userService;
    }

    @PostMapping("/friend_request")
    public ResponseEntity<Map<String, String>> friendRequest(
            @RequestParam(name = "user_id") Long user_id,
            @RequestParam(name = "action") String action,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userDetails == null ? null : userService.loadUserEntityByEmail(userDetails.getUsername());
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", "false", "message", "Not logged in"));
        }
        String result = friendService.handleFriendAction(user, user_id, action);
        boolean ok = !"invalid".equalsIgnoreCase(result);
        if (!ok) {
            return ResponseEntity.ok(Map.of("success", "false", "message", "Invalid friend action"));
        }
        return ResponseEntity.ok(Map.of("success", "true", "status", result));
    }
}
