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

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenBudgetTest {

    private PRContext ctxWithMethodsAndBudget(int n, int budget) {
        var methods = IntStream.range(0, n).mapToObj(i ->
            ChangedMethod.builder().filePath("F" + i + ".java").methodName("m" + i).build()
        ).toList();
        var defaults = CodemateConfig.defaults();
        var cfg = new CodemateConfig(defaults.version(), defaults.agents(), defaults.minSeverity(),
            defaults.maxCommentsPerPr(), defaults.minConfidence(), defaults.exclude(),
            defaults.customRules(),
            new CodemateConfig.Llm(defaults.llm().provider(), defaults.llm().model(), budget));
        return PRContext.builder().repoName("o/r").config(cfg).changedMethods(methods).build();
    }

    private ReviewAgent stub(String name, int tokensPerCall) {
        ReviewAgent a = mock(ReviewAgent.class);
        when(a.getName()).thenReturn(name);
        when(a.shouldRun(any())).thenReturn(true);
        when(a.reviewWithUsage(any(), any())).thenAnswer(inv -> {
            ChangedMethod m = inv.getArgument(0);
            var c = ReviewComment.builder()
                .agentName(name).filePath(m.filePath()).line(1)
                .severity(Severity.MEDIUM).category(ReviewCategory.BUG)
                .title("t").confidence(80).build();
            return new ReviewBatch(List.of(c), tokensPerCall);
        });
        return a;
    }

    @Test
    void overBudgetDisablesLowerPriorityAgents() {
        // Budget 1000. bug uses 400 across 2 methods = 800. security uses 400 across 2 methods = 800 total = 1600 > 1000.
        // After bug runs, used=800; budget check before security: 800 < 1000, run security. After security, used=1600.
        // Before performance: 1600 >= 1000, skip.
        var ctx = ctxWithMethodsAndBudget(2, 1000);
        var bug = stub("bug", 400);
        var sec = stub("security", 400);
        var perf = stub("performance", 400);
        var out = new ReviewOrchestrator(List.of(bug, sec, perf)).run(ctx);
        assertThat(out).extracting(ReviewComment::agentName)
            .containsOnly("bug", "security");
    }

    @Test
    void unlimitedBudgetRunsAllAgents() {
        var ctx = ctxWithMethodsAndBudget(2, 1_000_000);
        var bug = stub("bug", 100);
        var perf = stub("performance", 100);
        var out = new ReviewOrchestrator(List.of(bug, perf)).run(ctx);
        assertThat(out).extracting(ReviewComment::agentName)
            .containsOnly("bug", "performance");
    }
}
