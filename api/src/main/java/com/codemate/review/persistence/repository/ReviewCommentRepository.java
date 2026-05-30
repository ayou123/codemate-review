package com.codemate.review.persistence.repository;

import com.codemate.review.persistence.entity.ReviewCommentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewCommentRepository extends JpaRepository<ReviewCommentEntity, Long> {

    List<ReviewCommentEntity> findByReviewId(Long reviewId);
}
