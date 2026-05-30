package com.codemate.review.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_comments")
public class ReviewCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "agent_name", nullable = false, length = 50)
    private String agentName;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "category", nullable = false, length = 20)
    private String category;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "line", nullable = false)
    private Integer line;

    @Column(name = "title", columnDefinition = "text")
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "suggestion", columnDefinition = "text")
    private String suggestion;

    @Column(name = "confidence")
    private Integer confidence;

    @Column(name = "github_comment_id")
    private Long githubCommentId;

    public ReviewCommentEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public Long getGithubCommentId() {
        return githubCommentId;
    }

    public void setGithubCommentId(Long githubCommentId) {
        this.githubCommentId = githubCommentId;
    }
}
