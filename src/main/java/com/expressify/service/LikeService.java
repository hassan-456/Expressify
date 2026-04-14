package com.expressify.service;

import com.expressify.entity.Like;
import com.expressify.entity.Post;
import com.expressify.entity.User;
import com.expressify.repository.LikeRepository;
import com.expressify.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;

    public LikeService(LikeRepository likeRepository, PostRepository postRepository,
                       NotificationService notificationService) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public long toggleLike(Long postId, User user) {
        Post post = postRepository.findById(postId).orElseThrow();
        var existing = likeRepository.findByPostAndUser(post, user);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
        } else {
            Like like = new Like();
            like.setPost(post);
            like.setUser(user);
            likeRepository.save(like);
            if (!post.getUser().getId().equals(user.getId())) {
                notificationService.createNotification(post.getUser().getId(), "like",
                        user.getUsername() + " liked your post", user.getId(), postId);
            }
        }
        return likeRepository.countByPost(post);
    }

    public long getLikeCount(Long postId) {
        return postRepository.findById(postId).map(likeRepository::countByPost).orElse(0L);
    }
}
