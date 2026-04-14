package com.expressify.service;

import com.expressify.dto.PostDto;
import com.expressify.entity.Media;
import com.expressify.entity.Post;
import com.expressify.entity.User;
import com.expressify.repository.CommentRepository;
import com.expressify.repository.LikeRepository;
import com.expressify.repository.MediaRepository;
import com.expressify.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PostService {

    private static final String UPLOAD_DIR = "uploads/posts";

    private final PostRepository postRepository;
    private final MediaRepository mediaRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final ContentModerationService contentModerationService;

    public PostService(PostRepository postRepository, MediaRepository mediaRepository,
            LikeRepository likeRepository, CommentRepository commentRepository,
            NotificationService notificationService,
            ContentModerationService contentModerationService) {
        this.postRepository = postRepository;
        this.mediaRepository = mediaRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
        this.contentModerationService = contentModerationService;
    }

    @Transactional
    public Post createPost(User user, String content, MultipartFile mediaFile, java.time.LocalDateTime scheduledTime)
            throws IOException {
        if (contentModerationService.violatesGuidelines(content)) {
            throw new IllegalArgumentException("Your post was removed because it violates our content guidelines.");
        }

        Media media = null;
        if (mediaFile != null && !mediaFile.isEmpty()) {
            media = saveMedia(user, mediaFile);
        }

        Post post = new Post();
        post.setUser(user);
        post.setContent(content);
        post.setMedia(media);

        if (scheduledTime != null) {
            post.setScheduledFor(scheduledTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
        }

        return postRepository.save(post);
    }

    private Media saveMedia(User user, MultipartFile file) throws IOException {
        String ext = getExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "_"
                + System.currentTimeMillis() + "." + ext;
        Path dir = Paths.get(UPLOAD_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        Path path = dir.resolve(fileName);
        Files.copy(file.getInputStream(), path);
        Media media = new Media();
        media.setUser(user);
        media.setFileName(file.getOriginalFilename());
        // Store path as web-accessible URL (starts with /uploads/...)
        media.setFilePath("/" + UPLOAD_DIR + "/" + fileName);
        media.setFileType(file.getContentType() != null ? file.getContentType() : ext);
        media.setFileSize((int) file.getSize());
        return mediaRepository.save(media);
    }

    private String getExtension(String name) {
        if (name == null || !name.contains("."))
            return "jpg";
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }

    public List<PostDto> getFeedPosts(Long currentUserId, User currentUser) {
        Instant now = Instant.now();
        List<Post> posts = postRepository.findByScheduledForIsNullOrScheduledForBeforeOrderByCreatedAtDesc(now);
        return posts.stream()
                .map(p -> toPostDto(p, currentUserId))
                .collect(Collectors.toList());
    }

    public long countPostsByUser(User user) {
        return postRepository.countByUser(user);
    }

    public List<PostDto> getUserPosts(User targetUser, Long currentUserId) {
        Instant now = Instant.now();
        List<Post> posts = postRepository
                .findByUserAndScheduledForIsNullOrScheduledForBeforeOrderByCreatedAtDesc(targetUser, now);
        return posts.stream()
                .map(p -> toPostDto(p, currentUserId))
                .collect(Collectors.toList());
    }

    public PostDto toPostDto(Post post, Long currentUserId) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setUserId(post.getUser().getId());
        dto.setUsername(post.getUser().getUsername());
        dto.setProfilePicture(
                post.getUser().getProfilePicture() != null ? post.getUser().getProfilePicture() : "assets/logo.png");
        dto.setContent(post.getContent());
        dto.setCreatedAt(post.getCreatedAt());
        if (post.getMedia() != null) {
            dto.setFilePath(post.getMedia().getFilePath());
            dto.setFileType(post.getMedia().getFileType());
        }
        dto.setLikeCount(likeRepository.countByPost(post));
        dto.setCommentCount(commentRepository.countByPost(post));
        dto.setUserLiked(
                currentUserId != null && likeRepository.existsByPost_IdAndUser_Id(post.getId(), currentUserId));
        return dto;
    }

    public Post findById(Long id) {
        return postRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId).orElseThrow();
        if (!post.getUser().getId().equals(userId)) {
            throw new SecurityException("Not allowed to delete this post");
        }
        postRepository.delete(post);
    }
}
