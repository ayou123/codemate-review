package com.codemate.review.service;

import com.codemate.review.agent.orchestrator.ReviewOrchestrator;
import com.codemate.review.aggregator.ResultAggregator;
import com.codemate.review.core.config.CodemateConfigLoader;
import com.codemate.review.core.model.PRContext;
import com.codemate.review.core.model.ReviewResult;
import com.codemate.review.core.queue.ReviewJob;
import com.codemate.review.github.client.GitHubClient;
import com.codemate.review.github.client.PRInfo;
import com.codemate.review.github.publisher.CommentPublisher;
import com.codemate.review.parser.JavaCodeParser;
import com.codemate.review.parser.MethodExtractor;
import com.codemate.review.parser.PRDiffParser;
import com.codemate.review.parser.ProjectInfoDetector;
import com.codemate.review.persistence.entity.RepositoryEntity;
import com.codemate.review.persistence.entity.ReviewEntity;
import com.codemate.review.persistence.repository.RepositoryRepository;
import com.codemate.review.persistence.repository.ReviewCommentRepository;
import com.codemate.review.persistence.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewServiceTest {

    private GitHubClient gh;
    private ReviewOrchestrator orch;
    private ResultAggregator agg;
    private CommentPublisher pub;
    private RepositoryRepository repoRepo;
    private ReviewRepository revRepo;
    private ReviewCommentRepository ccRepo;
    private ReviewService svc;

    @BeforeEach
    void setup() {
        gh = mock(GitHubClient.class);
        orch = mock(ReviewOrchestrator.class);
        agg = mock(ResultAggregator.class);
        pub = mock(CommentPublisher.class);
        repoRepo = mock(RepositoryRepository.class);
        revRepo = mock(ReviewRepository.class);
        ccRepo = mock(ReviewCommentRepository.class);

        when(revRepo.save(any())).thenAnswer(inv -> {
            ReviewEntity r = inv.getArgument(0);
            if (r.getId() == null) r.setId(123L);
            return r;
        });

        RepositoryEntity repo = new RepositoryEntity();
        repo.setId(7L);
        repo.setFullName("o/r");
        when(repoRepo.findByFullName("o/r")).thenReturn(Optional.of(repo));

        svc = new ReviewService(gh, new PRDiffParser(),
            new MethodExtractor(new JavaCodeParser()), new ProjectInfoDetector(),
            orch, agg, pub, repoRepo, revRepo, ccRepo,
            new CodemateConfigLoader(), new ExclusionFilter(), mock(IndexingService.class));
    }

    @Test
    void endToEndPipelineForSinglePR() throws Exception {
        when(gh.fetchPR("o", "r", 1)).thenReturn(new PRInfo(1, "t", "b", "base", "head"));
        when(gh.fetchDiff("o", "r", 1)).thenReturn(
            "diff --git a/Foo.java b/Foo.java\nindex 1..2 100644\n--- a/Foo.java\n+++ b/Foo.java\n@@ -1,1 +1,1 @@\n+x\n");
        when(gh.fetchFiles(eq("o"), eq("r"), eq("head"), anyList())).thenReturn(Map.of("Foo.java", "class F{}"));
        when(gh.fetchFile(eq("o"), eq("r"), eq("head"), eq(".codemate.yml"))).thenReturn(Optional.empty());
        when(gh.fetchFile(eq("o"), eq("r"), eq("head"), eq("pom.xml"))).thenReturn(Optional.empty());
        when(gh.fetchFile(eq("o"), eq("r"), eq("head"), eq("build.gradle"))).thenReturn(Optional.empty());
        when(gh.fetchFile(eq("o"), eq("r"), eq("head"), eq("build.gradle.kts"))).thenReturn(Optional.empty());

        when(orch.run(any())).thenReturn(List.of());
        when(agg.aggregate(any(), any(), eq("head"))).thenReturn(ReviewResult.of(List.of()));

        svc.runReview(new ReviewJob("o/r", 1, "head", 42L));

        verify(pub).publish(eq("o"), eq("r"), eq(1), any(ReviewResult.class));
        ArgumentCaptor<ReviewEntity> captor = ArgumentCaptor.forClass(ReviewEntity.class);
        verify(revRepo, atLeast(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(ReviewEntity::getStatus)
            .contains("success");
    }

    @Test
    void failurePersistsFailedStatus() throws Exception {
        when(gh.fetchPR(any(), any(), anyInt())).thenThrow(new IOException("boom"));
        assertThatThrownBy(() -> svc.runReview(new ReviewJob("o/r", 1, "head", 42L)))
            .isInstanceOf(RuntimeException.class);
        ArgumentCaptor<ReviewEntity> captor = ArgumentCaptor.forClass(ReviewEntity.class);
        verify(revRepo, atLeast(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(ReviewEntity::getStatus).contains("failed");
    }

    @Test
    void skipsFilesLongerThan1000Lines() throws Exception {
        String big = IntStream.range(0, 1500).mapToObj(i -> "// " + i)
            .collect(Collectors.joining("\n"));
        when(gh.fetchPR(any(), any(), anyInt())).thenReturn(new PRInfo(1, "t", "b", "base", "head"));
        when(gh.fetchDiff(any(), any(), anyInt())).thenReturn(
            "diff --git a/Big.java b/Big.java\nindex 1..2 100644\n--- a/Big.java\n+++ b/Big.java\n@@ -1,1 +1,1 @@\n+x\n");
        when(gh.fetchFiles(any(), any(), any(), anyList())).thenReturn(Map.of("Big.java", big));
        when(gh.fetchFile(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(orch.run(any())).thenReturn(List.of());
        when(agg.aggregate(any(), any(), any())).thenReturn(ReviewResult.of(List.of()));

        svc.runReview(new ReviewJob("o/r", 1, "head", 42L));

        // Verify orchestrator was called with a PRContext whose changedMethods are empty
        // (Big.java was excluded because >1000 lines)
        ArgumentCaptor<PRContext> ctxCap = ArgumentCaptor.forClass(PRContext.class);
        verify(orch).run(ctxCap.capture());
        assertThat(ctxCap.getValue().changedMethods()).isEmpty();
    }
}
