package com.codemate.review.service;

import com.codemate.review.agent.orchestrator.ReviewOrchestrator;
import com.codemate.review.aggregator.ResultAggregator;
import com.codemate.review.core.config.CodemateConfig;
import com.codemate.review.core.config.CodemateConfigLoader;
import com.codemate.review.core.model.ChangedMethod;
import com.codemate.review.core.model.PRContext;
import com.codemate.review.core.model.ProjectInfo;
import com.codemate.review.core.model.ReviewComment;
import com.codemate.review.core.model.ReviewResult;
import com.codemate.review.core.queue.ReviewJob;
import com.codemate.review.github.client.GitHubClient;
import com.codemate.review.github.client.PRInfo;
import com.codemate.review.github.publisher.CommentPublisher;
import com.codemate.review.parser.DiffHunk;
import com.codemate.review.parser.MethodExtractor;
import com.codemate.review.parser.PRDiffParser;
import com.codemate.review.parser.ProjectInfoDetector;
import com.codemate.review.persistence.entity.RepositoryEntity;
import com.codemate.review.persistence.entity.ReviewCommentEntity;
import com.codemate.review.persistence.entity.ReviewEntity;
import com.codemate.review.persistence.repository.RepositoryRepository;
import com.codemate.review.persistence.repository.ReviewCommentRepository;
import com.codemate.review.persistence.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "codemate.queue.enabled", havingValue = "true", matchIfMissing = true)
public class ReviewService {
    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);
    private static final int MAX_FILE_LINES = 1000;

    private final GitHubClient gh;
    private final PRDiffParser diffParser;
    private final MethodExtractor methodExtractor;
    private final ProjectInfoDetector projectInfoDetector;
    private final ReviewOrchestrator orchestrator;
    private final ResultAggregator aggregator;
    private final CommentPublisher publisher;
    private final RepositoryRepository repoRepo;
    private final ReviewRepository revRepo;
    private final ReviewCommentRepository ccRepo;
    private final CodemateConfigLoader cfgLoader;
    private final ExclusionFilter exclusion;
    private final IndexingService indexingService;

    public ReviewService(GitHubClient gh, PRDiffParser diffParser, MethodExtractor methodExtractor,
                         ProjectInfoDetector projectInfoDetector, ReviewOrchestrator orchestrator,
                         ResultAggregator aggregator, CommentPublisher publisher,
                         RepositoryRepository repoRepo, ReviewRepository revRepo,
                         ReviewCommentRepository ccRepo, CodemateConfigLoader cfgLoader,
                         ExclusionFilter exclusion, IndexingService indexingService) {
        this.gh = gh;
        this.diffParser = diffParser;
        this.methodExtractor = methodExtractor;
        this.projectInfoDetector = projectInfoDetector;
        this.orchestrator = orchestrator;
        this.aggregator = aggregator;
        this.publisher = publisher;
        this.repoRepo = repoRepo;
        this.revRepo = revRepo;
        this.ccRepo = ccRepo;
        this.cfgLoader = cfgLoader;
        this.exclusion = exclusion;
        this.indexingService = indexingService;
    }

    public void runReview(ReviewJob job) {
        long t0 = System.currentTimeMillis();
        String[] or = job.repoFullName().split("/", 2);
        if (or.length != 2) throw new IllegalArgumentException("bad repoFullName: " + job.repoFullName());
        String owner = or[0];
        String repo = or[1];

        RepositoryEntity repoEnt = repoRepo.findByFullName(job.repoFullName()).orElseGet(() -> {
            RepositoryEntity r = new RepositoryEntity();
            r.setFullName(job.repoFullName());
            r.setInstallationId(job.installationId());
            r.setCreatedAt(LocalDateTime.now());
            return repoRepo.save(r);
        });

        ReviewEntity review = new ReviewEntity();
        review.setRepoId(repoEnt.getId());
        review.setPrNumber(job.prNumber());
        review.setCommitSha(job.headSha());
        review.setStatus("running");
        review.setCriticalCount(0);
        review.setHighCount(0);
        review.setMediumCount(0);
        review.setLowCount(0);
        review.setLlmTokensUsed(0);
        review.setLlmCostUsd(BigDecimal.ZERO);
        review.setCreatedAt(LocalDateTime.now());
        review = revRepo.save(review);

        String stage = "init";
        try {
            stage = "fetch-pr";
            PRInfo pr = gh.fetchPR(owner, repo, job.prNumber());

            stage = "index";
            indexingService.ensureIndexed(repoEnt, pr.headSha());

            stage = "load-config";
            CodemateConfig cfg = gh.fetchFile(owner, repo, pr.headSha(), ".codemate.yml")
                .map(cfgLoader::loadFromString)
                .orElseGet(CodemateConfig::defaults);

            stage = "fetch-diff";
            String diff = gh.fetchDiff(owner, repo, job.prNumber());
            List<DiffHunk> hunks = exclusion.filter(diffParser.parse(diff), cfg.exclude());

            stage = "fetch-files";
            List<String> paths = hunks.stream().map(DiffHunk::filePath).distinct().toList();
            Map<String, String> filesRaw = gh.fetchFiles(owner, repo, pr.headSha(), paths);
            Map<String, String> files = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : filesRaw.entrySet()) {
                long lc = e.getValue().lines().count();
                if (lc > MAX_FILE_LINES) {
                    log.info("skipping {} ({} lines > {})", e.getKey(), lc, MAX_FILE_LINES);
                    continue;
                }
                files.put(e.getKey(), e.getValue());
            }

            stage = "extract-methods";
            List<ChangedMethod> methods = methodExtractor.extract(hunks, files);

            stage = "project-info";
            Map<String, String> rootFiles = new HashMap<>();
            gh.fetchFile(owner, repo, pr.headSha(), "pom.xml").ifPresent(c -> rootFiles.put("pom.xml", c));
            gh.fetchFile(owner, repo, pr.headSha(), "build.gradle").ifPresent(c -> rootFiles.put("build.gradle", c));
            gh.fetchFile(owner, repo, pr.headSha(), "build.gradle.kts").ifPresent(c -> rootFiles.put("build.gradle.kts", c));
            ProjectInfo projectInfo = projectInfoDetector.detect(rootFiles);

            PRContext ctx = PRContext.builder()
                .repoName(job.repoFullName())
                .prNumber(job.prNumber())
                .prTitle(pr.title())
                .prDescription(pr.body())
                .baseSha(pr.baseSha())
                .headSha(pr.headSha())
                .projectInfo(projectInfo)
                .changedMethods(methods)
                .repoId(repoEnt.getId())
                .config(cfg)
                .build();

            stage = "agent-review";
            List<ReviewComment> raw = orchestrator.run(ctx);
            ReviewResult result = aggregator.aggregate(raw, cfg, pr.headSha());

            stage = "publish";
            publisher.publish(owner, repo, job.prNumber(), result);

            stage = "persist";
            persistComments(review.getId(), result);

            int durationMs = (int) (System.currentTimeMillis() - t0);
            review.setStatus("success");
            review.setOverallScore(result.overallScore());
            review.setCriticalCount(result.criticalCount());
            review.setHighCount(result.highCount());
            review.setMediumCount(result.mediumCount());
            review.setLowCount(result.lowCount());
            review.setDurationMs(durationMs);
            review.setFinishedAt(LocalDateTime.now());
            revRepo.save(review);

            log.info("[REVIEW-SUCCESS] {} pr#{} sha={} score={} crit={} high={} med={} low={} comments={} duration={}ms",
                job.repoFullName(), job.prNumber(), shortSha(pr.headSha()),
                result.overallScore(), result.criticalCount(), result.highCount(),
                result.mediumCount(), result.lowCount(), result.comments().size(), durationMs);
        } catch (Exception e) {
            int durationMs = (int) (System.currentTimeMillis() - t0);
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.error("[REVIEW-FAILED] {} pr#{} sha={} stage={} duration={}ms error={}",
                job.repoFullName(), job.prNumber(), shortSha(job.headSha()), stage, durationMs, msg, e);
            review.setStatus("failed");
            review.setErrorStage(stage);
            review.setErrorMessage(truncate(msg, 4000));
            review.setDurationMs(durationMs);
            review.setFinishedAt(LocalDateTime.now());
            revRepo.save(review);
            throw new RuntimeException(e);
        }
    }

    private static String shortSha(String sha) {
        return sha == null || sha.length() < 8 ? sha : sha.substring(0, 8);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private void persistComments(Long reviewId, ReviewResult result) {
        for (ReviewComment c : result.comments()) {
            ReviewCommentEntity e = new ReviewCommentEntity();
            e.setReviewId(reviewId);
            e.setAgentName(c.agentName());
            e.setSeverity(c.severity().name());
            e.setCategory(c.category().name());
            e.setFilePath(c.filePath());
            e.setLine(c.line());
            e.setTitle(c.title());
            e.setDescription(c.description());
            e.setSuggestion(c.suggestion());
            e.setConfidence(c.confidence());
            ccRepo.save(e);
        }
    }
}
