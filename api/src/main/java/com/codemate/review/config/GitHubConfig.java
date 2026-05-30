package com.codemate.review.config;

import com.codemate.review.core.queue.ReviewJob;
import com.codemate.review.github.client.GitHubClient;
import com.codemate.review.github.publisher.CommentPublisher;
import com.codemate.review.github.webhook.SignatureVerifier;
import com.codemate.review.github.webhook.WebhookDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GitHubConfig {

    private static final Logger log = LoggerFactory.getLogger(GitHubConfig.class);

    @Bean
    SignatureVerifier signatureVerifier(@Value("${codemate.github.webhook-secret:}") String secret) {
        return new SignatureVerifier(secret);
    }

    @Bean
    GitHubClient gitHubClient(@Value("${codemate.github.api-base}") String apiBase,
                              @Value("${codemate.github.app-token:}") String token) {
        return new GitHubClient(apiBase, token);
    }

    @Bean
    CommentPublisher commentPublisher(@Value("${codemate.github.api-base}") String apiBase,
                                      @Value("${codemate.github.app-token:}") String token) {
        return new CommentPublisher(apiBase, token);
    }

    /**
     * Placeholder dispatcher used until Task 19 wires the real Redis Stream impl.
     * Marked @ConditionalOnMissingBean so Task 19's real bean overrides it.
     */
    @Bean
    @ConditionalOnMissingBean(WebhookDispatcher.class)
    WebhookDispatcher noopWebhookDispatcher() {
        return (ReviewJob job) -> log.warn("placeholder dispatcher: dropping job for {} pr#{}",
            job.repoFullName(), job.prNumber());
    }
}
