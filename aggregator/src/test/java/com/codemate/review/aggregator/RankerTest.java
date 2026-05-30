package com.codemate.review.aggregator;

import com.codemate.review.core.enums.ReviewCategory;
import com.codemate.review.core.enums.Severity;
import com.codemate.review.core.model.ReviewComment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RankerTest {
    private ReviewComment c(Severity s, int conf) {
        return ReviewComment.builder().agentName("a").filePath("F.java").line(1)
                .severity(s).category(ReviewCategory.BUG).title("t").confidence(conf).build();
    }

    @Test
    void sortsBySeverityThenConfidenceDescending() {
        var low = c(Severity.LOW, 95);
        var crit = c(Severity.CRITICAL, 50);
        var high = c(Severity.HIGH, 80);
        var ranked = new Ranker().rank(List.of(low, crit, high));
        assertThat(ranked).extracting(ReviewComment::severity)
                .containsExactly(Severity.CRITICAL, Severity.HIGH, Severity.LOW);
    }

    @Test
    void sameSeverityRanksHigherConfidenceFirst() {
        var a = c(Severity.HIGH, 70);
        var b = c(Severity.HIGH, 90);
        var ranked = new Ranker().rank(List.of(a, b));
        assertThat(ranked.get(0).confidence()).isEqualTo(90);
    }
}
