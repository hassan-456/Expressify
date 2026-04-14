package com.expressify.repository;

import com.expressify.entity.Post;
import com.expressify.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    long countByUser(User user);

    List<Post> findByUserAndScheduledForIsNullOrderByCreatedAtDesc(User user);

    List<Post> findByUserAndScheduledForBeforeOrderByCreatedAtDesc(User user, Instant now);

    List<Post> findByUserAndScheduledForIsNullOrScheduledForBeforeOrderByCreatedAtDesc(User user, Instant now);

    List<Post> findByScheduledForIsNullOrScheduledForBeforeOrderByCreatedAtDesc(Instant now);

    List<Post> findTop5ByScheduledForIsNullOrScheduledForBeforeOrderByCreatedAtDesc(Instant now);

    List<Post> findByUserIdAndScheduledForIsNullOrderByCreatedAtDesc(Long userId);

    List<Post> findByUserIdAndScheduledForBeforeOrderByCreatedAtDesc(Long userId, Instant now);
}
