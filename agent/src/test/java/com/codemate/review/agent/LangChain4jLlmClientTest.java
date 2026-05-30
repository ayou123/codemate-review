package com.codemate.review.agent;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

class LangChain4jLlmClientTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().dynamicPort())
            .build();

    @Test
    void callsDeepSeekAndReturnsParsedJson() {
        wm.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson("""
                {"id":"x","object":"chat.completion","created":1234567890,"model":"deepseek-chat","choices":[{"index":0,"message":{"role":"assistant","content":"{\\"items\\":[{\\"title\\":\\"NPE\\"}]}"},"finish_reason":"stop"}],"usage":{"prompt_tokens":100,"completion_tokens":50,"total_tokens":1234}}""")));

        LlmClient c = LangChain4jLlmClient.deepseek("sk-test", wm.baseUrl(), "deepseek-chat");
        LlmResponse r = c.complete(new LlmRequest("sys", "usr", 0.2, 4000));

        assertThat(r.content()).contains("\"title\":\"NPE\"");
        assertThat(r.tokensUsed()).isEqualTo(1234);
        assertThat(c.providerName()).isEqualTo("deepseek");
    }
}
