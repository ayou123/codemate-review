package com.codemate.review.config;

import com.codemate.review.queue.ReviewJobConsumer;
import com.codemate.review.queue.ReviewJobProducer;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "codemate.queue.enabled", havingValue = "true", matchIfMissing = true)
public class RedisStreamConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamConfig.class);
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private StringRedisTemplate redis;

    @Bean
    StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamContainer(
            RedisConnectionFactory cf, ReviewJobConsumer consumer, StringRedisTemplate redis) {
        this.redis = redis;

        var opts = StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
            .pollTimeout(Duration.ofSeconds(1))
            .build();
        var c = StreamMessageListenerContainer.create(cf, opts);
        c.receive(Consumer.from(ReviewJobConsumer.CONSUMER_GROUP, "worker-1"),
            StreamOffset.create(ReviewJobProducer.STREAM, ReadOffset.lastConsumed()),
            consumer);
        this.container = c;
        return c;
    }

    @EventListener(ApplicationReadyEvent.class)
    void start() {
        if (redis == null || container == null) return;
        try {
            redis.opsForStream().createGroup(ReviewJobProducer.STREAM, ReadOffset.from("0"),
                ReviewJobConsumer.CONSUMER_GROUP);
        } catch (Exception e) {
            // BUSYGROUP: group already exists — ignore.
            log.debug("consumer group create skipped: {}", e.getMessage());
        }
        if (!container.isRunning()) {
            container.start();
            log.info("started Redis Stream listener container");
        }
    }

    @PreDestroy
    void stop() {
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }
}
