package com.codemate.review.rag.config;

import com.codemate.review.rag.embedding.EmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "codemate.rag.enabled", havingValue = "true")
public class RagConfig {
    @Bean
    EmbeddingService embeddingService(
            @Value("${codemate.rag.embedding.base-url:https://api.openai.com}") String baseUrl,
            @Value("${codemate.rag.embedding.api-key:}") String apiKey,
            @Value("${codemate.rag.embedding.model:text-embedding-3-small}") String model) {
        return new EmbeddingService(baseUrl, apiKey, model);
    }
}
