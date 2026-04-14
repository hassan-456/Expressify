package com.expressify.controller;

import com.expressify.dto.ChatMessageDto;
import com.expressify.entity.User;
import com.expressify.service.ChatService;
import com.expressify.service.UserService;
import com.expressify.service.MaasChatbotService;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MaasChatbotService maasService;

    public ChatWebSocketController(ChatService chatService, UserService userService,
            SimpMessagingTemplate messagingTemplate, @Lazy MaasChatbotService maasService) {
        this.chatService = chatService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
        this.maasService = maasService;
    }

    @MessageMapping("/chat.send")
    @SendToUser("/queue/messages")
    public ChatMessageDto sendMessage(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) {
            return null;
        }
        User sender = userService.loadUserEntityByEmail(principal.getName());
        if (sender == null) {
            return null;
        }
        Object receiverIdObj = payload.get("receiverId");
        Object contentObj = payload.get("content");
        if (receiverIdObj == null || contentObj == null) {
            return null;
        }
        Long receiverId = Long.valueOf(receiverIdObj.toString());
        String content = contentObj.toString();

        ChatMessageDto dto = chatService.sendMessage(sender.getId(), receiverId, content);

        // Deliver to receiver
        User receiver = userService.findById(receiverId);
        if (receiver != null) {
            // Check if receiver is the MAAS AI Chatbot
            if ("maas@expressify.com".equals(receiver.getEmail())) {
                maasService.processMessage(sender, receiver.getId(), content);
            } else {
                messagingTemplate.convertAndSendToUser(
                        receiver.getEmail(),
                        "/queue/messages",
                        dto);
            }
        }

        // Echo back to sender (the @SendToUser return value)
        return dto;
    }
}
