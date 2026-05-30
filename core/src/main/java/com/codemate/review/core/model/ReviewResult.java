package com.codemate.review.core.model;

import com.codemate.review.core.enums.Severity;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public record ReviewResult(
        @JsonProperty("headSha") String headSha,
        @JsonProperty("comments") List<ReviewComment> comments,
        @JsonProperty("overallScore") int overallScore,
        @JsonProperty("criticalCount") int criticalCount,
        @JsonProperty("highCount") int highCount,
        @JsonProperty("mediumCount") int mediumCount,
        @JsonProperty("lowCount") int lowCount,
        @JsonProperty("llmTokensUsed") int llmTokensUsed,
        @JsonProperty("durationMs") long durationMs
) {
    public ReviewResult {
        comments = comments == null ? List.of() : List.copyOf(comments);
    }

    /** Builds a result from comments only; {@code headSha=null}, {@code llmTokensUsed=0}, {@code durationMs=0} are placeholders for the caller to set later. */
    public static ReviewResult of(List<ReviewComment> comments) {
        List<ReviewComment> safe = comments == null ? List.of() : comments;
        int critical = 0, high = 0, medium = 0, low = 0;
        for (ReviewComment c : safe) {
            if (c.severity() == Severity.CRITICAL) critical++;
            else if (c.severity() == Severity.HIGH) high++;
            else if (c.severity() == Severity.MEDIUM) medium++;
            else if (c.severity() == Severity.LOW) low++;
        }
        int score = Math.max(0, 100 - critical * 20 - high * 10 - medium * 5 - low);
        return new ReviewResult(null, safe, score, critical, high, medium, low, 0, 0L);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String headSha;
        private List<ReviewComment> comments = new ArrayList<>();
        private int overallScore;
        private int criticalCount;
        private int highCount;
        private int mediumCount;
        private int lowCount;
        private int llmTokensUsed;
        private long durationMs;

        public Builder headSha(String v) { this.headSha = v; return this; }
        public Builder comments(List<ReviewComment> v) { this.comments = v; return this; }
        public Builder overallScore(int v) { this.overallScore = v; return this; }
        public Builder criticalCount(int v) { this.criticalCount = v; return this; }
        public Builder highCount(int v) { this.highCount = v; return this; }
        public Builder mediumCount(int v) { this.mediumCount = v; return this; }
        public Builder lowCount(int v) { this.lowCount = v; return this; }
        public Builder llmTokensUsed(int v) { this.llmTokensUsed = v; return this; }
        public Builder durationMs(long v) { this.durationMs = v; return this; }

        public ReviewResult build() {
            return new ReviewResult(headSha, comments, overallScore, criticalCount, highCount,
                    mediumCount, lowCount, llmTokensUsed, durationMs);
        }
    }
}
