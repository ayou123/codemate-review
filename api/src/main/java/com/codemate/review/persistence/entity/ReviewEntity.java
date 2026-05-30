package com.codemate.review.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_id")
    private Long repoId;

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(name = "commit_sha", nullable = false, length = 40)
    private String commitSha;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "critical_count")
    private Integer criticalCount;

    @Column(name = "high_count")
    private Integer highCount;

    @Column(name = "medium_count")
    private Integer mediumCount;

    @Column(name = "low_count")
    private Integer lowCount;

    @Column(name = "llm_tokens_used")
    private Integer llmTokensUsed;

    @Column(name = "llm_cost_usd", precision = 10, scale = 4)
    private BigDecimal llmCostUsd;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "error_stage", length = 50)
    private String errorStage;

    public ReviewEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepoId() {
        return repoId;
    }

    public void setRepoId(Long repoId) {
        this.repoId = repoId;
    }

    public Integer getPrNumber() {
        return prNumber;
    }

    public void setPrNumber(Integer prNumber) {
        this.prNumber = prNumber;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }

    public Integer getCriticalCount() {
        return criticalCount;
    }

    public void setCriticalCount(Integer criticalCount) {
        this.criticalCount = criticalCount;
    }

    public Integer getHighCount() {
        return highCount;
    }

    public void setHighCount(Integer highCount) {
        this.highCount = highCount;
    }

    public Integer getMediumCount() {
        return mediumCount;
    }

    public void setMediumCount(Integer mediumCount) {
        this.mediumCount = mediumCount;
    }

    public Integer getLowCount() {
        return lowCount;
    }

    public void setLowCount(Integer lowCount) {
        this.lowCount = lowCount;
    }

    public Integer getLlmTokensUsed() {
        return llmTokensUsed;
    }

    public void setLlmTokensUsed(Integer llmTokensUsed) {
        this.llmTokensUsed = llmTokensUsed;
    }

    public BigDecimal getLlmCostUsd() {
        return llmCostUsd;
    }

    public void setLlmCostUsd(BigDecimal llmCostUsd) {
        this.llmCostUsd = llmCostUsd;
    }

    public Integer getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorStage() {
        return errorStage;
    }

    public void setErrorStage(String errorStage) {
        this.errorStage = errorStage;
    }
}
