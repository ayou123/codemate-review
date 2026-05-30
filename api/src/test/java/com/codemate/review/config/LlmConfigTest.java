package com.codemate.review.config;

import com.codemate.review.agent.LlmClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies provider-conditional LlmClient wiring. Two top-level test
 * classes in the same file: each is a {@code @SpringBootTest} with its
 * own provider property, exercising the {@code @ConditionalOnProperty}
 * branches in {@link LlmConfig}.
 */
class LlmConfigTest {

    @Test
    void smoke() {
        // Marker so surefire still runs this top-level class and
        // ensures the file compiles even if both profile tests are skipped.
        assertThat(true).isTrue();
    }
}

@SpringBootTest(properties = {
        "codemate.llm.provider=deepseek",
        "codemate.llm.deepseek.api-key=sk-test",
        "codemate.llm.deepseek.base-url=http://localhost",
        "codemate.llm.deepseek.model=deepseek-chat"
})
@ActiveProfiles("test")
class LlmConfigDeepSeekProfileTest {

    @Autowired
    LlmClient client;

    @Test
    void usesDeepSeekClient() {
        assertThat(client.providerName()).isEqualTo("deepseek");
    }
}

@SpringBootTest(properties = {
        "codemate.llm.provider=qwen",
        "codemate.llm.qwen.api-key=sk-test",
        "codemate.llm.qwen.model=qwen-max"
})
@ActiveProfiles("test")
class LlmConfigQwenProfileTest {

    @Autowired
    LlmClient client;

    @Test
    void usesQwenClient() {
        assertThat(client.providerName()).isEqualTo("qwen");
    }
}
