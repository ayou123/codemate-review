package com.codemate.review.github.publisher;

import com.codemate.review.core.enums.ReviewCategory;
import com.codemate.review.core.enums.Severity;
import com.codemate.review.core.model.ReviewComment;
import com.codemate.review.core.model.ReviewResult;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

class CommentPublisherTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance().options(options().dynamicPort()).build();

    private ReviewComment c(String file, int line, Severity sev) {
        return ReviewComment.builder()
            .agentName("bug").filePath(file).line(line)
            .severity(sev).category(ReviewCategory.BUG)
            .title("issue at " + line)
            .description("desc")
            .suggestion("sug")
            .suggestedCode("code")
            .confidence(90)
            .references(List.of("CWE-1"))
            .build();
    }

    @Test
    void postsReviewAndStatus() throws Exception {
        wm.stubFor(post(urlEqualTo("/repos/o/r/pulls/1/reviews"))
            .willReturn(okJson("{\"id\":42}")));
        wm.stubFor(post(urlMatching("/repos/o/r/statuses/.*"))
            .willReturn(okJson("{}")));

        var crit = c("Foo.java", 42, Severity.CRITICAL);
        var med = c("Foo.java", 60, Severity.MEDIUM);
        var low = c("Foo.java", 70, Severity.LOW);
        var result = ReviewResult.builder()
            .headSha("abc123")
            .comments(List.of(crit, med, low))
            .criticalCount(1).highCount(0).mediumCount(1).lowCount(1)
            .overallScore(70).llmTokensUsed(0).durationMs(0L).build();

        new CommentPublisher(wm.baseUrl(), "tok").publish("o","r",1, result);

        wm.verify(postRequestedFor(urlEqualTo("/repos/o/r/pulls/1/reviews"))
            .withRequestBody(matchingJsonPath("$.event", equalTo("COMMENT")))
            .withRequestBody(matchingJsonPath("$.comments[0].path", equalTo("Foo.java")))
            .withRequestBody(matchingJsonPath("$.comments[0].line", equalTo("42")))
            .withRequestBody(matchingJsonPath("$.body", containing("CodeMate Review"))));
        wm.verify(postRequestedFor(urlEqualTo("/repos/o/r/statuses/abc123"))
            .withRequestBody(matchingJsonPath("$.state", equalTo("failure"))));
    }

    @Test
    void onlyCriticalAndHighBecomeInlineComments() throws Exception {
        wm.stubFor(post(urlEqualTo("/repos/o/r/pulls/2/reviews"))
            .willReturn(okJson("{\"id\":43}")));
        wm.stubFor(post(urlMatching("/repos/o/r/statuses/.*"))
            .willReturn(okJson("{}")));

        var med = c("F.java", 1, Severity.MEDIUM);
        var low = c("F.java", 2, Severity.LOW);
        var result = ReviewResult.builder()
            .headSha("def456")
            .comments(List.of(med, low))
            .criticalCount(0).highCount(0).mediumCount(1).lowCount(1)
            .overallScore(94).llmTokensUsed(0).durationMs(0L).build();

        new CommentPublisher(wm.baseUrl(), "tok").publish("o","r",2, result);

        wm.verify(postRequestedFor(urlEqualTo("/repos/o/r/pulls/2/reviews"))
            .withRequestBody(matchingJsonPath("$.comments", equalToJson("[]"))));
    }

    @Test
    void emptyResultStillPostsApprovalAndSuccessStatus() throws Exception {
        wm.stubFor(post(urlEqualTo("/repos/o/r/pulls/3/reviews"))
            .willReturn(okJson("{\"id\":44}")));
        wm.stubFor(post(urlMatching("/repos/o/r/statuses/.*"))
            .willReturn(okJson("{}")));

        var result = ReviewResult.builder()
            .headSha("ghi789")
            .comments(List.of())
            .criticalCount(0).highCount(0).mediumCount(0).lowCount(0)
            .overallScore(100).llmTokensUsed(0).durationMs(0L).build();

        new CommentPublisher(wm.baseUrl(), "tok").publish("o","r",3, result);

        wm.verify(postRequestedFor(urlEqualTo("/repos/o/r/statuses/ghi789"))
            .withRequestBody(matchingJsonPath("$.state", equalTo("success"))));
        wm.verify(postRequestedFor(urlEqualTo("/repos/o/r/pulls/3/reviews"))
            .withRequestBody(matchingJsonPath("$.body", containing("未发现问题"))));
    }
}
