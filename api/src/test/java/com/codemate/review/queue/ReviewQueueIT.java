package com.codemate.review.queue;

import com.codemate.review.core.queue.ReviewJob;
import com.codemate.review.service.ReviewService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
@Tag("docker")
class ReviewQueueIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        r.add("spring.flyway.enabled", () -> "false");
        // Disable DB autoconfig — this IT only needs Redis
        r.add("spring.autoconfigure.exclude",
            () -> "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration");
    }

    @Autowired ReviewJobProducer producer;
    @Autowired StringRedisTemplate redisTemplate;
    @SpyBean ReviewService reviewService;

    @Test
    void enqueueIsHandledByConsumer() {
        doNothing().when(reviewService).runReview(any());
        producer.enqueue(new ReviewJob("o/r", 1, "abc", 42L));
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
            verify(reviewService).runReview(argThat(j -> j.prNumber() == 1)));
    }

    @Test
    void consumerSkipsStaleJob() throws Exception {
        doNothing().when(reviewService).runReview(any());
        redisTemplate.opsForValue().set("cancel:o/r:99", "sha2");
        producer.enqueue(new ReviewJob("o/r", 99, "sha1", 42L));
        // give the consumer a chance to process — verify NEVER called with prNumber 99
        Thread.sleep(2000);
        verify(reviewService, never()).runReview(argThat(j -> j.prNumber() == 99));
    }
}
