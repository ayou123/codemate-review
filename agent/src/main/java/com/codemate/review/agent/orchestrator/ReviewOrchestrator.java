package com.codemate.review.agent.orchestrator;

import com.codemate.review.agent.ReviewAgent;
import com.codemate.review.agent.ReviewBatch;
import com.codemate.review.core.model.ChangedMethod;
import com.codemate.review.core.model.PRContext;
import com.codemate.review.core.model.ReviewComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ReviewOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(ReviewOrchestrator.class);
    private static final List<String> PRIORITY = List.of("bug", "security", "performance", "style", "design");

    private final List<ReviewAgent> agents;

    public ReviewOrchestrator(List<ReviewAgent> agents) {
        this.agents = List.copyOf(agents);
    }

    public List<ReviewComment> run(PRContext ctx) {
        int budget = (ctx.config() != null && ctx.config().llm() != null)
            ? ctx.config().llm().maxTokensPerReview()
            : Integer.MAX_VALUE;
        AtomicInteger used = new AtomicInteger(0);

        Map<String, ReviewAgent> byName = agents.stream()
            .collect(Collectors.toMap(ReviewAgent::getName, a -> a, (a, b) -> a));

        List<ReviewComment> all = new ArrayList<>();
        for (String name : PRIORITY) {
            ReviewAgent a = byName.get(name);
            if (a == null || !a.shouldRun(ctx)) continue;
            if (used.get() >= budget) {
                log.info("budget exhausted ({}/{} tokens); skipping {}", used.get(), budget, name);
                continue;
            }
            try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<ReviewBatch>> futures = new ArrayList<>();
                for (ChangedMethod m : ctx.changedMethods()) {
                    futures.add(exec.submit(() -> safeReview(a, m, ctx)));
                }
                for (Future<ReviewBatch> f : futures) {
                    ReviewBatch b = getQuietly(f);
                    all.addAll(b.comments());
                    used.addAndGet(b.tokensUsed());
                }
            }
        }
        return all;
    }

    private ReviewBatch safeReview(ReviewAgent a, ChangedMethod m, PRContext ctx) {
        try {
            return a.reviewWithUsage(m, ctx);
        } catch (Exception e) {
            log.warn("agent {} failed on {}.{}: {}", a.getName(), m.className(), m.methodName(), e.getMessage());
            return ReviewBatch.empty();
        }
    }

    private static ReviewBatch getQuietly(Future<ReviewBatch> f) {
        try { return f.get(); }
        catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return ReviewBatch.empty();
        }
    }
}
