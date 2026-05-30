package com.codemate.review.config;

import com.codemate.review.agent.LangChain4jLlmClient;
import com.codemate.review.agent.LlmClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class LlmConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "codemate.llm.provider", havingValue = "deepseek", matchIfMissing = true)
    LlmClient deepseekClient(
            @Value("${codemate.llm.deepseek.api-key:}") String apiKey,
            @Value("${codemate.llm.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${codemate.llm.deepseek.model:deepseek-chat}") String model) {
        return LangChain4jLlmClient.deepseek(apiKey, baseUrl, model);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "codemate.llm.provider", havingValue = "qwen")
    LlmClient qwenClient(
            @Value("${codemate.llm.qwen.api-key:}") String apiKey,
            @Value("${codemate.llm.qwen.model:qwen-max}") String model) {
        return LangChain4jLlmClient.qwen(apiKey, model);
    }
}
