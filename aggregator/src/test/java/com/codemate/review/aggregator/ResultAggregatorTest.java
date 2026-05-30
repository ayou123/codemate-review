package com.codemate.review.aggregator;

import com.codemate.review.core.config.CodemateConfig;
import com.codemate.review.core.enums.ReviewCategory;
import com.codemate.review.core.enums.Severity;
import com.codemate.review.core.model.ReviewComment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ResultAggregatorTest {
    private ReviewComment c(int line, Severity s, int conf) {
        return ReviewComment.builder().agentName("a").filePath("F.java").line(line)
                .severity(s).category(ReviewCategory.BUG).title("t").confidence(conf).build();
    }

    private final ResultAggregator agg = new ResultAggregator(new Deduplicator(), new Ranker());

    @Test
    void appliesConfidenceFloorAndMaxCap() {
        var cfg = CodemateConfig.defaults();  // minConfidence=70, maxCommentsPerPr=20
        var raw = IntStream.range(0, 30)
                .mapToObj(i -> c(i, Severity.HIGH, i < 10 ? 60 : 80))
                .toList();
        var r = agg.aggregate(raw, cfg, "sha");
        assertThat(r.comments()).hasSize(20);
        assertThat(r.comments()).allMatch(x -> x.confidence() >= 70);
    }

    @Test
    void computesScoreAndCounts() {
        var raw = List.of(
                c(1, Severity.CRITICAL, 90),
                c(2, Severity.HIGH, 80),
                c(3, Severity.LOW, 80));
        var r = agg.aggregate(raw, CodemateConfig.defaults(), "sha");
        assertThat(r.criticalCount()).isEqualTo(1);
        assertThat(r.highCount()).isEqualTo(1);
        assertThat(r.lowCount()).isEqualTo(1);
        assertThat(r.overallScore()).isBetween(0, 100);
    }

    @Test
    void emptyInputProducesPerfectScore() {
        var r = agg.aggregate(List.of(), CodemateConfig.defaults(), "sha");
        assertThat(r.comments()).isEmpty();
        assertThat(r.overallScore()).isEqualTo(100);
        assertThat(r.headSha()).isEqualTo("sha");
    }
}
