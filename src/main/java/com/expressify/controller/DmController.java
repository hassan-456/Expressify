package com.expressify.controller;

import com.expressify.dto.ChatMessageDto;
import com.expressify.entity.FriendRequest;
import com.expressify.entity.User;
import com.expressify.repository.FriendRequestRepository;
import com.expressify.service.ChatService;
import com.expressify.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dm")
public class DmController {

    private final UserService userService;
    private final FriendRequestRepository friendRequestRepository;
    private final ChatService chatService;

    public DmController(UserService userService,
            FriendRequestRepository friendRequestRepository,
            ChatService chatService) {
        this.userService = userService;
        this.friendRequestRepository = friendRequestRepository;
        this.chatService = chatService;
    }

    private User requireUser(UserDetails details) {
        if (details == null)
            return null;
        return userService.loadUserEntityByEmail(details.getUsername());
    }

    @GetMapping("/friends")
    public ResponseEntity<?> getFriends(@AuthenticationPrincipal UserDetails userDetails) {
        User current = requireUser(userDetails);
        if (current == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Not logged in"));
        }
        List<FriendRequest> accepted = friendRequestRepository.findAcceptedByUser(current);
        List<Map<String, Object>> friends = accepted.stream()
                .map(fr -> fr.getSender().getId().equals(current.getId()) ? fr.getReceiver() : fr.getSender())
                .distinct()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "username", u.getUsername(),
                        "profilePicture", u.getProfilePicture()))
                .collect(Collectors.toList());

        // Inject MAAS AI Chatbot into the friends list
        User maasUser = userService.loadUserEntityByEmail("maas@expressify.com");
        if (maasUser != null && !maasUser.getId().equals(current.getId())) {
            // make the list mutable
            friends = new java.util.ArrayList<>(friends);
            friends.add(0, Map.<String, Object>of(
                    "id", maasUser.getId(),
                    "username", maasUser.getUsername(),
                    "profilePicture", maasUser.getProfilePicture()));
        }

        return ResponseEntity.ok(Map.of("success", true, "friends", friends));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestParam(name = "friendId") Long friendId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User current = requireUser(userDetails);
        if (current == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Not logged in"));
        }
        List<ChatMessageDto> messages = chatService.getConversation(current.getId(), friendId);
        return ResponseEntity.ok(Map.of("success", true, "messages", messages));
    }
}
