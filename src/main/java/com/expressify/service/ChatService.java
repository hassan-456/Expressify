package com.expressify.service;

import com.expressify.dto.ChatMessageDto;
import com.expressify.entity.ChatMessage;
import com.expressify.entity.User;
import com.expressify.repository.ChatMessageRepository;
import com.expressify.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatService(ChatMessageRepository chatMessageRepository, UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChatMessageDto sendMessage(Long senderId, Long receiverId, String content) {
        User sender = userRepository.findById(senderId).orElseThrow();
        User receiver = userRepository.findById(receiverId).orElseThrow();

        ChatMessage msg = new ChatMessage();
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setContent(content != null ? content.trim() : "");
        msg = chatMessageRepository.save(msg);
        return toDto(msg);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getConversation(Long userId, Long friendId) {
        User user = userRepository.findById(userId).orElseThrow();
        User friend = userRepository.findById(friendId).orElseThrow();
        List<ChatMessage> list = chatMessageRepository
                .findBySenderAndReceiverOrSenderAndReceiverOrderByCreatedAtAsc(
                        user, friend, friend, user);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    private ChatMessageDto toDto(ChatMessage msg) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(msg.getId());
        dto.setSenderId(msg.getSender().getId());
        dto.setReceiverId(msg.getReceiver().getId());
        dto.setSenderName(msg.getSender().getUsername());
        dto.setContent(msg.getContent());
        dto.setCreatedAt(msg.getCreatedAt());
        return dto;
    }
}

