package com.expressify.repository;

import com.expressify.entity.ChatMessage;
import com.expressify.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySenderAndReceiverOrSenderAndReceiverOrderByCreatedAtAsc(
            User sender1, User receiver1, User sender2, User receiver2);

    void deleteBySenderAndReceiverOrSenderAndReceiver(
            User sender1, User receiver1, User sender2, User receiver2);
}
