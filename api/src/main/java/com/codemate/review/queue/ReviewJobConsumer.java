package com.codemate.review.queue;

import com.codemate.review.core.queue.ReviewJob;
import com.codemate.review.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "codemate.queue.enabled", havingValue = "true", matchIfMissing = true)
public class ReviewJobConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    public static final String CONSUMER_GROUP = "codemate-workers";
    private static final Logger log = LoggerFactory.getLogger(ReviewJobConsumer.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final ReviewService service;

    public ReviewJobConsumer(StringRedisTemplate redis, ObjectMapper json, ReviewService service) {
        this.redis = redis;
        this.json = json;
        this.service = service;
    }

    public StringRedisTemplate redisTemplate() { return redis; }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            String payload = message.getValue().get("payload");
            ReviewJob job = json.readValue(payload, ReviewJob.class);
            String latestSha = redis.opsForValue().get("cancel:" + job.repoFullName() + ":" + job.prNumber());
            if (latestSha != null && !latestSha.equals(job.headSha())) {
                log.info("skipping stale job for {} pr#{} sha={}",
                    job.repoFullName(), job.prNumber(), job.headSha());
            } else {
                service.runReview(job);
            }
            redis.opsForStream().acknowledge(CONSUMER_GROUP, message);
        } catch (Exception e) {
            log.error("consumer failure", e);
        }
    }
}
