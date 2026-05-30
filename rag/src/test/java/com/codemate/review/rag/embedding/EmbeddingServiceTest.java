package com.codemate.review.rag.embedding;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingServiceTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
        .options(options().dynamicPort()).build();

    @Test
    void returnsVectorForSingleText() {
        wm.stubFor(post(urlEqualTo("/v1/embeddings"))
            .willReturn(okJson("""
                {"data":[{"embedding":[0.1,0.2,0.3]}],"usage":{"total_tokens":3}}""")));
        float[] v = new EmbeddingService(wm.baseUrl(), "sk-test", "text-embedding-3-small").embed("hello");
        assertThat(v).hasSize(3);
        assertThat(v[0]).isEqualTo(0.1f);
        assertThat(v[1]).isEqualTo(0.2f);
        assertThat(v[2]).isEqualTo(0.3f);
    }

    @Test
    void embedsBatch() {
        wm.stubFor(post(urlEqualTo("/v1/embeddings"))
            .willReturn(okJson("""
                {"data":[{"embedding":[0.1]},{"embedding":[0.2]}]}""")));
        var vs = new EmbeddingService(wm.baseUrl(), "sk", "m").embedBatch(List.of("a", "b"));
        assertThat(vs).hasSize(2);
        assertThat(vs.get(0)[0]).isEqualTo(0.1f);
        assertThat(vs.get(1)[0]).isEqualTo(0.2f);
    }

    @Test
    void nonSuccessThrows() {
        wm.stubFor(post(urlEqualTo("/v1/embeddings"))
            .willReturn(aResponse().withStatus(500).withBody("err")));
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            new EmbeddingService(wm.baseUrl(), "sk", "m").embed("x"))
            .isInstanceOf(RuntimeException.class);
    }
}
