package com.codemate.review.github.webhook;

import com.codemate.review.core.queue.ReviewJob;

public interface WebhookDispatcher {
    void enqueue(ReviewJob job);
}
