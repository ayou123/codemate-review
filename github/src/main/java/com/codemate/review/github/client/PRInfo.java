package com.codemate.review.github.client;

public record PRInfo(int number, String title, String body, String baseSha, String headSha) {
    public PRInfo {
        title = title == null ? "" : title;
        body = body == null ? "" : body;
    }
}
