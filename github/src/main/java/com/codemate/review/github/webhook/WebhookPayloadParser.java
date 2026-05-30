package com.codemate.review.github.webhook;

import com.codemate.review.core.queue.ReviewJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class WebhookPayloadParser {
    private static final Set<String> ACCEPTED_ACTIONS = Set.of("opened", "synchronize", "reopened");
    private final ObjectMapper json = new ObjectMapper();

    public Optional<ReviewJob> parsePR(String body) {
        try {
            JsonNode n = json.readTree(body);
            String action = n.path("action").asText("");
            if (!ACCEPTED_ACTIONS.contains(action)) return Optional.empty();
            String repo = n.path("repository").path("full_name").asText("");
            int pr = n.path("pull_request").path("number").asInt(0);
            String sha = n.path("pull_request").path("head").path("sha").asText("");
            long installationId = n.path("installation").path("id").asLong(0L);
            if (repo.isEmpty() || pr == 0 || sha.isEmpty()) return Optional.empty();
            return Optional.of(new ReviewJob(repo, pr, sha, installationId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
