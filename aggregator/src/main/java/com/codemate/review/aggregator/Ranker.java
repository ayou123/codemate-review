package com.codemate.review.aggregator;

import com.codemate.review.core.model.ReviewComment;

import java.util.Comparator;
import java.util.List;

public class Ranker {
    public List<ReviewComment> rank(List<ReviewComment> in) {
        return in.stream()
                .sorted(Comparator.<ReviewComment>comparingInt(
                        c -> c.severity().getRank() * 1000 + c.confidence()).reversed())
                .toList();
    }
}
