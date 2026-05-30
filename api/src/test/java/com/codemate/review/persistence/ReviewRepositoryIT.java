package com.codemate.review.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.codemate.review.persistence.entity.RepositoryEntity;
import com.codemate.review.persistence.entity.ReviewCommentEntity;
import com.codemate.review.persistence.entity.ReviewEntity;
import com.codemate.review.persistence.repository.RepositoryRepository;
import com.codemate.review.persistence.repository.ReviewCommentRepository;
import com.codemate.review.persistence.repository.ReviewRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Testcontainers
@Tag("docker")
class ReviewRepositoryIT {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @Autowired
    RepositoryRepository repoRepo;

    @Autowired
    ReviewRepository reviewRepo;

    @Autowired
    ReviewCommentRepository commentRepo;

    @Test
    void persistsRepositoryReviewAndComments() {
        var repo = new RepositoryEntity();
        repo.setFullName("o/r");
        repo.setGithubId(12345L);
        repo.setInstallationId(7L);
        repo = repoRepo.save(repo);

        var review = new ReviewEntity();
        review.setRepoId(repo.getId());
        review.setPrNumber(1);
        review.setCommitSha("0000000000000000000000000000000000000001");
        review.setStatus("success");
        review.setOverallScore(90);
        review.setCriticalCount(0);
        review.setHighCount(1);
        review.setMediumCount(2);
        review.setLowCount(3);
        review.setLlmTokensUsed(1000);
        review.setLlmCostUsd(BigDecimal.ZERO);
        review.setDurationMs(5000);
        review = reviewRepo.save(review);

        var comment = new ReviewCommentEntity();
        comment.setReviewId(review.getId());
        comment.setAgentName("bug");
        comment.setSeverity("HIGH");
        comment.setCategory("BUG");
        comment.setFilePath("Foo.java");
        comment.setLine(42);
        comment.setTitle("NPE");
        comment.setDescription("desc");
        comment.setSuggestion("guard");
        comment.setConfidence(90);
        commentRepo.save(comment);

        assertThat(commentRepo.findByReviewId(review.getId())).hasSize(1);
        assertThat(repoRepo.findByFullName("o/r")).isPresent();
    }
}
