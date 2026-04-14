package com.expressify.service;

import com.expressify.entity.FriendRequest;
import com.expressify.entity.User;
import com.expressify.repository.FriendRequestRepository;
import com.expressify.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FriendService(FriendRequestRepository friendRequestRepository, UserRepository userRepository,
                         NotificationService notificationService) {
        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public enum FriendshipStatus { NONE, PENDING_SENT, PENDING_RECEIVED, FRIENDS }

    public List<UserWithStatus> getAllUsersWithStatus(User currentUser) {
        List<User> all = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .sorted(Comparator.comparing(User::getUsername))
                .collect(Collectors.toList());
        List<FriendRequest> myRequests = friendRequestRepository.findBySenderOrReceiver(currentUser);
        Map<Long, FriendshipStatus> statusMap = new HashMap<>();
        for (FriendRequest fr : myRequests) {
            Long otherId = fr.getSender().getId().equals(currentUser.getId())
                    ? fr.getReceiver().getId() : fr.getSender().getId();
            if (fr.getStatus() == FriendRequest.FriendRequestStatus.accepted) {
                statusMap.put(otherId, FriendshipStatus.FRIENDS);
            } else if (fr.getSender().getId().equals(currentUser.getId())) {
                statusMap.put(otherId, FriendshipStatus.PENDING_SENT);
            } else {
                statusMap.put(otherId, FriendshipStatus.PENDING_RECEIVED);
            }
        }
        return all.stream()
                .map(u -> new UserWithStatus(u, statusMap.getOrDefault(u.getId(), FriendshipStatus.NONE)))
                .collect(Collectors.toList());
    }

    public List<UserWithStatus> searchUsersWithStatus(User currentUser, String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String q = query.toLowerCase();
        return getAllUsersWithStatus(currentUser).stream()
                .filter(uw -> uw.getUser().getUsername() != null &&
                        uw.getUser().getUsername().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    @Transactional
    public String handleFriendAction(User currentUser, Long otherUserId, String action) {
        User other = userRepository.findById(otherUserId).orElseThrow();
        Optional<FriendRequest> opt = friendRequestRepository.findBySenderAndReceiver(currentUser, other);
        if (opt.isEmpty()) {
            opt = friendRequestRepository.findBySenderAndReceiver(other, currentUser);
        }
        FriendRequest fr = opt.orElse(null);

        return switch (action.toLowerCase()) {
            case "send" -> {
                if (fr != null) yield "Request already exists";
                FriendRequest newReq = new FriendRequest();
                newReq.setSender(currentUser);
                newReq.setReceiver(other);
                friendRequestRepository.save(newReq);
                notificationService.createNotification(other.getId(), "friend_request",
                        currentUser.getUsername() + " sent you a friend request", currentUser.getId(), null);
                yield "sent";
            }
            case "accept" -> {
                if (fr == null || !fr.getReceiver().getId().equals(currentUser.getId())) yield "invalid";
                fr.setStatus(FriendRequest.FriendRequestStatus.accepted);
                friendRequestRepository.save(fr);
                notificationService.createNotification(fr.getSender().getId(), "friend_accepted",
                        currentUser.getUsername() + " accepted your friend request", currentUser.getId(), null);
                yield "accepted";
            }
            case "cancel", "reject" -> {
                if (fr != null) {
                    friendRequestRepository.delete(fr);
                }
                yield "cancelled";
            }
            case "unfriend" -> {
                if (fr != null && fr.getStatus() == FriendRequest.FriendRequestStatus.accepted) {
                    friendRequestRepository.delete(fr);
                    yield "unfriended";
                }
                yield "invalid";
            }
            default -> "invalid";
        };
    }

    public int getFriendCount(User user) {
        return friendRequestRepository.findAcceptedByUser(user).size();
    }

    public static class UserWithStatus {
        private final User user;
        private final FriendshipStatus status;

        public UserWithStatus(User user, FriendshipStatus status) {
            this.user = user;
            this.status = status;
        }
        public User getUser() { return user; }
        public FriendshipStatus getStatus() { return status; }
    }
}
