package com.expressify.controller;

import com.expressify.entity.Post;
import com.expressify.entity.User;
import com.expressify.repository.PostRepository;
import com.expressify.repository.ReportRepository;
import com.expressify.repository.UserRepository;
import com.expressify.entity.Report;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

        private final UserRepository userRepository;
        private final PostRepository postRepository;
        private final ReportRepository reportRepository;

        public AdminController(UserRepository userRepository, PostRepository postRepository,
                        ReportRepository reportRepository) {
                this.userRepository = userRepository;
                this.postRepository = postRepository;
                this.reportRepository = reportRepository;
        }

        @GetMapping({ "", "/", "/dashboard" })
        public String dashboard(Model model) {
                long totalUsers = userRepository.count();
                long newUsersToday = userRepository.countByCreatedAtAfter(Instant.now().truncatedTo(ChronoUnit.DAYS));
                long totalPosts = postRepository.count();
                long totalReports = reportRepository.count();

                Instant now = Instant.now();
                List<User> recentUsers = userRepository.findTop5ByOrderByCreatedAtDesc();
                List<Post> recentPosts = postRepository
                                .findTop5ByScheduledForIsNullOrScheduledForBeforeOrderByCreatedAtDesc(now);

                Map<Long, Long> reportCounts = new HashMap<>();
                for (Post p : recentPosts) {
                        long count = reportRepository.countByContentTypeAndContentId(Report.ContentType.post,
                                        p.getId());
                        reportCounts.put(p.getId(), count);
                }

                model.addAttribute("totalUsers", totalUsers);
                model.addAttribute("newUsersToday", newUsersToday);
                model.addAttribute("totalPosts", totalPosts);
                model.addAttribute("totalReports", totalReports);
                model.addAttribute("recentUsers", recentUsers);
                model.addAttribute("recentPosts", recentPosts);
                model.addAttribute("reportCounts", reportCounts);

                return "admin/dashboard";
        }

        @GetMapping("/users")
        public String users(Model model) {
                List<User> users = userRepository.findAll();
                model.addAttribute("users", users);
                return "admin/users";
        }

        @GetMapping("/users/{id}")
        public String viewUser(@PathVariable(name = "id") Long id, Model model) {
                return userRepository.findById(id).map(user -> {
                        model.addAttribute("user", user);

                        // To emulate the real profile page for the admin, we need the user's posts
                        Instant now = Instant.now();
                        List<Post> posts = postRepository.findByUserIdAndScheduledForBeforeOrderByCreatedAtDesc(id,
                                        now);
                        model.addAttribute("posts", posts);

                        // We should ideally return a view that looks like the profile but under admin/
                        // or just use the existing profile.html if we add admin to the model.
                        // The simplest approach is to reuse the user's profile view if it doesn't
                        // strictly check for Principal.
                        // Let's create an `admin/user-profile.html` to be safe and clean.
                        return "admin/user-profile";
                }).orElse("redirect:/admin/users");
        }

        @PostMapping("/users/ban")
        public String toggleBanUser(@RequestParam(name = "id") Long id) {
                userRepository.findById(id).ifPresent(user -> {
                        if (user.getStatus() == User.UserStatus.banned) {
                                user.setStatus(User.UserStatus.active);
                        } else {
                                user.setStatus(User.UserStatus.banned);
                        }
                        userRepository.save(user);
                });
                return "redirect:/admin/users";
        }

        @jakarta.persistence.PersistenceContext
        private jakarta.persistence.EntityManager entityManager;

        @org.springframework.transaction.annotation.Transactional
        @PostMapping("/users/delete")
        public String deleteUser(@RequestParam(name = "id") Long id) {
                // ===== Correct FK-safe deletion order =====

                // 1. Delete reports by this user
                entityManager.createQuery("DELETE FROM Report r WHERE r.reporterId = :id")
                                .setParameter("id", id).executeUpdate();

                // 2. Delete reports targeting this user's posts
                entityManager.createNativeQuery(
                                "DELETE FROM reports WHERE content_type = 'post' AND content_id IN (SELECT id FROM posts WHERE user_id = ?)")
                                .setParameter(1, id).executeUpdate();

                // 3. Delete comment_likes on comments made by this user, or by this user
                entityManager.createQuery("DELETE FROM CommentLike cl WHERE cl.user.id = :id")
                                .setParameter("id", id).executeUpdate();
                entityManager.createNativeQuery(
                                "DELETE FROM comment_likes WHERE comment_id IN (SELECT id FROM comments WHERE user_id = ?)")
                                .setParameter(1, id).executeUpdate();

                // 4. Delete comment_likes on comments of user's posts
                entityManager.createNativeQuery(
                                "DELETE FROM comment_likes WHERE comment_id IN (SELECT c.id FROM comments c JOIN posts p ON c.post_id = p.id WHERE p.user_id = ?)")
                                .setParameter(1, id).executeUpdate();

                // 5. Delete likes on user's posts (by anyone) and likes by this user (on any
                // post)
                entityManager.createNativeQuery(
                                "DELETE FROM likes WHERE post_id IN (SELECT id FROM posts WHERE user_id = ?)")
                                .setParameter(1, id).executeUpdate();
                entityManager.createQuery("DELETE FROM Like l WHERE l.user.id = :id")
                                .setParameter("id", id).executeUpdate();

                // 6. Null out media_id in comments before deleting comments/media
                entityManager.createNativeQuery(
                                "UPDATE comments SET media_id = NULL WHERE user_id = ?")
                                .setParameter(1, id).executeUpdate();
                entityManager.createNativeQuery(
                                "UPDATE comments SET media_id = NULL WHERE post_id IN (SELECT id FROM posts WHERE user_id = ?)")
                                .setParameter(1, id).executeUpdate();

                // 7. Delete comments on user's posts (by anyone) and comments by this user (on
                // any post)
                entityManager.createNativeQuery(
                                "DELETE FROM comments WHERE post_id IN (SELECT id FROM posts WHERE user_id = ?)")
                                .setParameter(1, id).executeUpdate();
                entityManager.createQuery("DELETE FROM Comment c WHERE c.user.id = :id")
                                .setParameter("id", id).executeUpdate();

                // 8. Delete post_hashtags for user's posts
                entityManager.createNativeQuery(
                                "DELETE FROM post_hashtags WHERE post_id IN (SELECT id FROM posts WHERE user_id = ?)")
                                .setParameter(1, id).executeUpdate();

                // 9. Null out media_id in posts BEFORE deleting media
                entityManager.createNativeQuery("UPDATE posts SET media_id = NULL WHERE user_id = ?")
                                .setParameter(1, id).executeUpdate();

                // 10. Delete user's posts
                entityManager.createNativeQuery("DELETE FROM posts WHERE user_id = ?")
                                .setParameter(1, id).executeUpdate();

                // 11. Now safe to delete media
                entityManager.createQuery("DELETE FROM Media m WHERE m.user.id = :id")
                                .setParameter("id", id).executeUpdate();

                // 12. Preserve chat messages but null out the sender reference
                // so the message text stays in the other person's DM
                entityManager.createNativeQuery("UPDATE chat_messages SET sender_id = NULL WHERE sender_id = ?")
                                .setParameter(1, id).executeUpdate();
                entityManager.createNativeQuery("UPDATE chat_messages SET receiver_id = NULL WHERE receiver_id = ?")
                                .setParameter(1, id).executeUpdate();

                // 13. Delete friend requests
                entityManager.createQuery("DELETE FROM FriendRequest f WHERE f.sender.id = :id OR f.receiver.id = :id")
                                .setParameter("id", id).executeUpdate();

                // 14. Delete notifications
                entityManager.createQuery("DELETE FROM Notification n WHERE n.user.id = :id")
                                .setParameter("id", id).executeUpdate();

                // 15. Delete settings
                entityManager.createQuery("DELETE FROM Settings s WHERE s.user.id = :id")
                                .setParameter("id", id).executeUpdate();

                // 16. Finally delete the user
                entityManager.createNativeQuery("DELETE FROM users WHERE id = ?")
                                .setParameter(1, id).executeUpdate();

                return "redirect:/admin/users";
        }

        @GetMapping("/posts")
        public String posts(Model model) {
                Instant now = Instant.now();
                List<Post> posts = postRepository.findByScheduledForIsNullOrScheduledForBeforeOrderByCreatedAtDesc(now);
                Map<Long, Long> reportCounts = new HashMap<>();
                for (Post p : posts) {
                        long count = reportRepository.countByContentTypeAndContentId(Report.ContentType.post,
                                        p.getId());
                        reportCounts.put(p.getId(), count);
                }

                model.addAttribute("posts", posts);
                model.addAttribute("reportCounts", reportCounts);
                return "admin/posts";
        }

        @GetMapping("/posts/{id}")
        public String viewPost(@PathVariable(name = "id") Long id, Model model) {
                return postRepository.findById(id).map(post -> {
                        model.addAttribute("post", post);
                        long reportCount = reportRepository.countByContentTypeAndContentId(Report.ContentType.post,
                                        post.getId());
                        model.addAttribute("reportCount", reportCount);
                        return "admin/post-detail";
                }).orElse("redirect:/admin/posts");
        }

        @PostMapping("/posts/feature")
        public String featurePost(@RequestParam(name = "id") Long id) {
                postRepository.findById(id).ifPresent(post -> {
                        post.setFeatured(!Boolean.TRUE.equals(post.getFeatured()));
                        postRepository.save(post);
                });
                return "redirect:/admin/posts";
        }

        @PostMapping("/posts/delete")
        public String deletePost(@RequestParam(name = "id") Long id) {
                postRepository.deleteById(id);
                return "redirect:/admin/posts";
        }

        @GetMapping("/content-analysis")
        public String contentAnalysis(Model model) {
                return "admin/content-analysis";
        }
}
