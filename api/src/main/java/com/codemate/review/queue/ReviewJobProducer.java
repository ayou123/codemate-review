package com.codemate.review.queue;

import com.codemate.review.core.queue.ReviewJob;
import com.codemate.review.github.webhook.WebhookDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "codemate.queue.enabled", havingValue = "true", matchIfMissing = true)
public class ReviewJobProducer implements WebhookDispatcher {

    public static final String STREAM = "codemate.reviews";
    private static final Logger log = LoggerFactory.getLogger(ReviewJobProducer.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper json;

    public ReviewJobProducer(StringRedisTemplate redis, ObjectMapper json) {
        this.redis = redis;
        this.json = json;
    }

    @Override
    public void enqueue(ReviewJob job) {
        try {
            String cancelKey = "cancel:" + job.repoFullName() + ":" + job.prNumber();
            redis.opsForValue().set(cancelKey, job.headSha());

            MapRecord<String, String, String> record = MapRecord.create(STREAM,
                Map.of("payload", json.writeValueAsString(job)));
            redis.opsForStream().add(record);
            log.info("enqueued review {} pr#{}", job.repoFullName(), job.prNumber());
        } catch (Exception e) {
            throw new RuntimeException("failed to enqueue ReviewJob", e);
        }
    }
}
