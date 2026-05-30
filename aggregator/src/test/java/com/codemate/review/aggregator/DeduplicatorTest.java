package com.codemate.review.aggregator;

import com.codemate.review.core.enums.ReviewCategory;
import com.codemate.review.core.enums.Severity;
import com.codemate.review.core.model.ReviewComment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeduplicatorTest {
    private ReviewComment c(String agent, String file, int line, Severity sev, ReviewCategory cat, int conf) {
        return ReviewComment.builder().agentName(agent).filePath(file).line(line)
                .severity(sev).category(cat).title(agent + " finding").confidence(conf).build();
    }

    @Test
    void mergesSameFileLineCategory() {
        var a = c("bug", "Foo.java", 42, Severity.HIGH, ReviewCategory.BUG, 85);
        var b = c("style", "Foo.java", 42, Severity.HIGH, ReviewCategory.BUG, 90);
        var out = new Deduplicator().dedup(List.of(a, b));
        assertThat(out).hasSize(1);
        assertThat(out.get(0).confidence()).isEqualTo(90);
    }

    @Test
    void differentLinesKept() {
        var a = c("bug", "Foo.java", 42, Severity.HIGH, ReviewCategory.BUG, 85);
        var b = c("bug", "Foo.java", 43, Severity.HIGH, ReviewCategory.BUG, 85);
        assertThat(new Deduplicator().dedup(List.of(a, b))).hasSize(2);
    }

    @Test
    void differentCategoriesKept() {
        var a = c("bug", "Foo.java", 42, Severity.HIGH, ReviewCategory.BUG, 85);
        var b = c("sec", "Foo.java", 42, Severity.HIGH, ReviewCategory.SECURITY, 85);
        assertThat(new Deduplicator().dedup(List.of(a, b))).hasSize(2);
    }
}
