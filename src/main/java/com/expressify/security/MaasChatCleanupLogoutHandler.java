package com.expressify.security;

import com.expressify.entity.User;
import com.expressify.repository.ChatMessageRepository;
import com.expressify.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MaasChatCleanupLogoutHandler implements LogoutHandler {

    private final ChatMessageRepository chatMessageRepository;
    private final CustomUserDetailsService userDetailsService;

    public MaasChatCleanupLogoutHandler(ChatMessageRepository chatMessageRepository,
            CustomUserDetailsService userDetailsService) {
        this.chatMessageRepository = chatMessageRepository;
        this.userDetailsService = userDetailsService;
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return;
        }

        User currentUser = userDetailsService.loadUserEntityByEmail(authentication.getName());
        User maasUser = userDetailsService.loadUserEntityByEmail("maas@expressify.com");

        if (currentUser != null && maasUser != null) {
            // Delete all chats between the logging-out user and the MAAS AI
            chatMessageRepository.deleteBySenderAndReceiverOrSenderAndReceiver(
                    currentUser, maasUser, maasUser, currentUser);
        }
    }
}
