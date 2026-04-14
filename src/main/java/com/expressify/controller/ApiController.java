package com.expressify.controller;

import com.expressify.dto.CommentDto;
import com.expressify.entity.User;
import com.expressify.service.CommentService;
import com.expressify.service.LikeService;
import com.expressify.service.PostService;
import com.expressify.service.ReportService;
import com.expressify.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final LikeService likeService;
    private final CommentService commentService;
    private final PostService postService;
    private final UserService userService;
    private final ReportService reportService;

    public ApiController(LikeService likeService, CommentService commentService,
            PostService postService, UserService userService,
            ReportService reportService) {
        this.likeService = likeService;
        this.commentService = commentService;
        this.postService = postService;
        this.userService = userService;
        this.reportService = reportService;
    }

    private User requireUser(UserDetails details) {
        if (details == null)
            return null;
        return userService.loadUserEntityByEmail(details.getUsername());
    }

    @GetMapping("/likes")
    public ResponseEntity<Map<String, Object>> likeAction(
            @RequestParam(name = "action") String action,
            @RequestParam(name = "post_id") Long post_id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = requireUser(userDetails);
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Not logged in"));
        }
        try {
            long count = likeService.toggleLike(post_id, user);
            return ResponseEntity.ok(Map.of("success", true, "count", count));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/get_comments")
    public ResponseEntity<Map<String, Object>> getComments(
            @RequestParam(name = "post_id") Long post_id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = requireUser(userDetails);
        Long userId = user != null ? user.getId() : null;
        List<CommentDto> comments = commentService.getComments(post_id, userId);
        return ResponseEntity.ok(Map.of("success", true, "comments", comments));
    }

    @PostMapping("/comments")
    public ResponseEntity<Map<String, Object>> addComment(
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "post_id", required = false) Long post_id,
            @RequestParam(name = "comment_id", required = false) Long comment_id,
            @RequestParam(name = "comment", required = false) String comment,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = requireUser(userDetails);
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Not logged in"));
        }
        if ("add".equals(action) && post_id != null) {
            CommentDto dto = commentService.addComment(post_id, user, comment != null ? comment.trim() : "");
            return ResponseEntity.ok(Map.of("success", true, "comment", dto));
        }
        if ("delete".equals(action) && comment_id != null) {
            commentService.deleteComment(comment_id, user.getId());
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.ok(Map.of("success", false, "message", "Invalid request"));
    }

    @PostMapping("/delete_post")
    public ResponseEntity<Map<String, Object>> deletePost(
            @RequestParam(name = "post_id") Long post_id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = requireUser(userDetails);
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "error", "Not logged in"));
        }
        try {
            postService.deletePost(post_id, user.getId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/report_post")
    public ResponseEntity<Map<String, Object>> reportPost(
            @RequestParam(name = "post_id") Long post_id,
            @RequestParam(name = "reason") String reason,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = requireUser(userDetails);
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "error", "Not logged in"));
        }
        try {
            reportService.reportPost(user, post_id, reason);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Thanks for your report. Our team will review this post.");
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", "Failed to submit report."));
        }
    }
}
