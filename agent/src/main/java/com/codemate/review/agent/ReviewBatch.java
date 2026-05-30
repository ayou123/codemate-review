package com.codemate.review.agent;

import com.codemate.review.core.model.ReviewComment;

import java.util.List;

public record ReviewBatch(List<ReviewComment> comments, int tokensUsed) {
    public ReviewBatch {
        comments = comments == null ? List.of() : List.copyOf(comments);
    }

    public static ReviewBatch empty() {
        return new ReviewBatch(List.of(), 0);
    }
}
