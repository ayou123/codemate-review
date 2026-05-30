package com.codemate.review.github.webhook;

import com.codemate.review.core.queue.ReviewJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class WebhookController {
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final SignatureVerifier verifier;
    private final WebhookDispatcher dispatcher;
    private final WebhookPayloadParser parser;

    public WebhookController(SignatureVerifier verifier,
                             WebhookDispatcher dispatcher,
                             WebhookPayloadParser parser) {
        this.verifier = verifier;
        this.dispatcher = dispatcher;
        this.parser = parser;
    }

    @PostMapping("/webhook/github")
    public ResponseEntity<Void> handle(
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String body) {
        if (!verifier.verify(body, signature)) {
            log.warn("rejected webhook: bad signature");
            return ResponseEntity.status(401).build();
        }
        if (!"pull_request".equals(event)) {
            return ResponseEntity.noContent().build();
        }
        Optional<ReviewJob> job = parser.parsePR(body);
        if (job.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        dispatcher.enqueue(job.get());
        log.info("enqueued review for {} pr#{}", job.get().repoFullName(), job.get().prNumber());
        return ResponseEntity.accepted().build();
    }
}
