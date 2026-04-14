package com.expressify.service;

import com.expressify.dto.CommentDto;
import com.expressify.entity.Comment;
import com.expressify.entity.Post;
import com.expressify.entity.User;
import com.expressify.repository.CommentRepository;
import com.expressify.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository,
                          NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public CommentDto addComment(Long postId, User user, String content) {
        Post post = postRepository.findById(postId).orElseThrow();
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);
        comment = commentRepository.save(comment);
        if (!post.getUser().getId().equals(user.getId())) {
            notificationService.createNotification(post.getUser().getId(), "comment",
                    user.getUsername() + " commented on your post", user.getId(), postId);
        }
        return toCommentDto(comment, user.getId());
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        if (!comment.getUser().getId().equals(userId)) {
            throw new SecurityException("Not allowed to delete this comment");
        }
        commentRepository.delete(comment);
    }

    public List<CommentDto> getComments(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId).orElseThrow();
        return commentRepository.findByPostOrderByCreatedAtDesc(post).stream()
                .map(c -> toCommentDto(c, currentUserId))
                .collect(Collectors.toList());
    }

    private CommentDto toCommentDto(Comment c, Long currentUserId) {
        CommentDto dto = new CommentDto();
        dto.setId(c.getId());
        dto.setPostId(c.getPost().getId());
        dto.setUserId(c.getUser().getId());
        dto.setUsername(c.getUser().getUsername());
        dto.setProfilePicture(c.getUser().getProfilePicture() != null ? c.getUser().getProfilePicture() : "assets/logo.png");
        dto.setContent(c.getContent());
        dto.setComment(c.getContent());
        dto.setCanDelete(c.getUser().getId().equals(currentUserId));
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }
}
