package com.codemate.review.aggregator;

import com.codemate.review.core.config.CodemateConfig;
import com.codemate.review.core.enums.Severity;
import com.codemate.review.core.model.ReviewComment;
import com.codemate.review.core.model.ReviewResult;

import java.util.List;

public class ResultAggregator {
    private final Deduplicator dedup;
    private final Ranker ranker;

    public ResultAggregator(Deduplicator d, Ranker r) {
        this.dedup = d;
        this.ranker = r;
    }

    public ReviewResult aggregate(List<ReviewComment> raw, CodemateConfig cfg, String headSha) {
        var filtered = raw.stream().filter(c -> c.confidence() >= cfg.minConfidence()).toList();
        var deduped = dedup.dedup(filtered);
        var ranked = ranker.rank(deduped);
        var capped = ranked.size() > cfg.maxCommentsPerPr()
                ? ranked.subList(0, cfg.maxCommentsPerPr())
                : ranked;
        int crit = (int) capped.stream().filter(c -> c.severity() == Severity.CRITICAL).count();
        int high = (int) capped.stream().filter(c -> c.severity() == Severity.HIGH).count();
        int med  = (int) capped.stream().filter(c -> c.severity() == Severity.MEDIUM).count();
        int low  = (int) capped.stream().filter(c -> c.severity() == Severity.LOW).count();
        int score = Math.max(0, 100 - crit * 20 - high * 10 - med * 5 - low);
        return ReviewResult.builder()
                .headSha(headSha)
                .comments(capped)
                .criticalCount(crit).highCount(high).mediumCount(med).lowCount(low)
                .overallScore(score)
                .llmTokensUsed(0)
                .durationMs(0L)
                .build();
    }
}
