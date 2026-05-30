package com.codemate.review.queue;

import com.codemate.review.core.queue.ReviewJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewJobProducerTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void enqueueAddsToStreamAndUpdatesCancelKey() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        StreamOperations<String, Object, Object> streams = mock(StreamOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForStream()).thenReturn((StreamOperations) streams);

        var producer = new ReviewJobProducer(redis, new ObjectMapper());
        producer.enqueue(new ReviewJob("o/r", 1, "abc", 42L));

        verify(values).set(eq("cancel:o/r:1"), eq("abc"));
        verify(streams).add(any(MapRecord.class));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void duplicatePREnqueueOverwritesCancelKeyWithLatestSha() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        StreamOperations<String, Object, Object> streams = mock(StreamOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForStream()).thenReturn((StreamOperations) streams);

        var producer = new ReviewJobProducer(redis, new ObjectMapper());
        producer.enqueue(new ReviewJob("o/r", 1, "sha1", 42L));
        producer.enqueue(new ReviewJob("o/r", 1, "sha2", 42L));

        verify(values).set("cancel:o/r:1", "sha1");
        verify(values).set("cancel:o/r:1", "sha2");
    }
}
