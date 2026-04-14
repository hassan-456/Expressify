package com.expressify.repository;

import com.expressify.entity.Like;
import com.expressify.entity.Post;
import com.expressify.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByPostAndUser(Post post, User user);

    long countByPost(Post post);

    void deleteByPostAndUser(Post post, User user);

    boolean existsByPost_IdAndUser_Id(Long postId, Long userId);
}
