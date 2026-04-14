package com.expressify.repository;

import com.expressify.entity.Comment;
import com.expressify.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostOrderByCreatedAtDesc(Post post);

    long countByPost(Post post);
}
