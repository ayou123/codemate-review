package com.codemate.review.core.model;

import com.codemate.review.core.enums.ReviewCategory;
import com.codemate.review.core.enums.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewCommentTest {
    @Test
    void roundTripJson() throws Exception {
        var c = ReviewComment.builder()
                .agentName("BugAgent").filePath("Foo.java").line(42)
                .severity(Severity.HIGH).category(ReviewCategory.BUG)
                .title("NPE risk").description("...").suggestion("guard null")
                .suggestedCode("if (u==null) throw ...").confidence(92)
                .references(List.of("CWE-476")).build();
        var mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(c);
        ReviewComment back = mapper.readValue(json, ReviewComment.class);
        assertThat(back).usingRecursiveComparison().isEqualTo(c);
    }
}
