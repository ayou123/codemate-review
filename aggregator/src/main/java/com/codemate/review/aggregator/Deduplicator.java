package com.codemate.review.aggregator;

import com.codemate.review.core.model.ReviewComment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Deduplicator {
    public List<ReviewComment> dedup(List<ReviewComment> in) {
        Map<String, ReviewComment> byKey = new LinkedHashMap<>();
        for (ReviewComment c : in) {
            String k = c.filePath() + ":" + c.line() + ":" + c.category();
            byKey.merge(k, c, (a, b) -> a.confidence() >= b.confidence() ? a : b);
        }
        return new ArrayList<>(byKey.values());
    }
}
