package com.codemate.review.core.queue;

public record ReviewJob(String repoFullName, int prNumber, String headSha, long installationId) {}
