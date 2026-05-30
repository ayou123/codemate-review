package com.codemate.review.agent.orchestrator;

import com.codemate.review.agent.ReviewAgent;
import com.codemate.review.agent.ReviewBatch;
import com.codemate.review.core.config.CodemateConfig;
import com.codemate.review.core.enums.ReviewCategory;
import com.codemate.review.core.enums.Severity;
import com.codemate.review.core.model.ChangedMethod;
import com.codemate.review.core.model.PRContext;
import com.codemate.review.core.model.ReviewComment;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewOrchestratorTest {

    private PRContext ctxWithMethods(int n) {
        var methods = IntStream.range(0, n).mapToObj(i -> ChangedMethod.builder()
            .filePath("F" + i + ".java").methodName("m" + i).build()).toList();
        return PRContext.builder()
            .repoName("o/r")
            .config(CodemateConfig.defaults())
            .changedMethods(methods)
            .build();
    }

    private ReviewAgent stubAgent(String name, int commentsPerMethod) {
        ReviewAgent a = mock(ReviewAgent.class);
        when(a.shouldRun(any())).thenReturn(true);
        when(a.getName()).thenReturn(name);
        when(a.reviewWithUsage(any(), any())).thenAnswer((Answer<ReviewBatch>) inv -> {
            ChangedMethod m = inv.getArgument(0);
            List<ReviewComment> list = new ArrayList<>();
            for (int i = 0; i < commentsPerMethod; i++) {
                list.add(ReviewComment.builder()
                    .agentName(name).filePath(m.filePath()).line(10 + i)
                    .severity(Severity.MEDIUM).category(ReviewCategory.BUG)
                    .title(name + " finding " + i).confidence(80).build());
            }
            return new ReviewBatch(list, 100);
        });
        return a;
    }

    @Test
    void runsAllAgentsAcrossAllMethodsInParallel() {
        var bug = stubAgent("bug", 5);
        var sec = stubAgent("security", 2);
        var ctx = ctxWithMethods(3);
        var out = new ReviewOrchestrator(List.of(bug, sec)).run(ctx);
        assertThat(out).hasSize((5 + 2) * 3);
    }

    @Test
    void disabledAgentsAreSkipped() {
        var bug = stubAgent("bug", 5);
        when(bug.shouldRun(any())).thenReturn(false);
        assertThat(new ReviewOrchestrator(List.of(bug)).run(ctxWithMethods(1))).isEmpty();
    }

    @Test
    void agentExceptionDoesNotFailBatch() {
        var ok = stubAgent("bug", 1);
        ReviewAgent bad = mock(ReviewAgent.class);
        when(bad.shouldRun(any())).thenReturn(true);
        when(bad.getName()).thenReturn("security");
        when(bad.reviewWithUsage(any(), any())).thenThrow(new RuntimeException("boom"));
        var out = new ReviewOrchestrator(List.of(ok, bad)).run(ctxWithMethods(1));
        assertThat(out).hasSize(1);
    }

    @Test
    void noMethodsProducesEmptyResult() {
        var bug = stubAgent("bug", 5);
        assertThat(new ReviewOrchestrator(List.of(bug)).run(ctxWithMethods(0))).isEmpty();
    }
}
