package com.expressify.repository;

import com.expressify.entity.FriendRequest;
import com.expressify.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    Optional<FriendRequest> findBySenderAndReceiver(User sender, User receiver);

    @Query("SELECT fr FROM FriendRequest fr WHERE (fr.sender = :user OR fr.receiver = :user) AND fr.status = 'accepted'")
    List<FriendRequest> findAcceptedByUser(@Param("user") User user);

    List<FriendRequest> findByReceiverAndStatus(User receiver, FriendRequest.FriendRequestStatus status);

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.sender = :user OR fr.receiver = :user")
    List<FriendRequest> findBySenderOrReceiver(@Param("user") User user);
}
