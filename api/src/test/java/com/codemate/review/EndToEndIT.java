package com.codemate.review;

import com.codemate.review.persistence.repository.ReviewRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("docker")
class EndToEndIT {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    static WireMockServer github;
    static WireMockServer llm;

    @BeforeAll
    static void startWireMock() {
        github = new WireMockServer(0); github.start();
        llm = new WireMockServer(0); llm.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (github != null) github.stop();
        if (llm != null) llm.stop();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("codemate.queue.enabled", () -> "true");
        r.add("codemate.github.api-base", () -> github.baseUrl());
        r.add("codemate.github.app-token", () -> "fake");
        r.add("codemate.github.webhook-secret", () -> "topsecret");
        r.add("codemate.llm.provider", () -> "deepseek");
        r.add("codemate.llm.deepseek.base-url", () -> llm.baseUrl());
        r.add("codemate.llm.deepseek.api-key", () -> "sk-test");
    }

    @Autowired TestRestTemplate restTemplate;
    @Autowired ReviewRepository reviewRepo;

    private String fixture(String name) throws Exception {
        return Files.readString(Path.of("src/test/resources/fixtures/e2e/" + name));
    }

    private String hmac(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] out = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : out) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    @Test
    void webhookTriggersReviewAndPostsComments() throws Exception {
        github.stubFor(get(urlEqualTo("/repos/o/r/pulls/1"))
            .willReturn(okJson(fixture("pr.json"))));
        github.stubFor(get(urlEqualTo("/repos/o/r/pulls/1"))
            .withHeader("Accept", equalTo("application/vnd.github.v3.diff"))
            .willReturn(aResponse().withStatus(200).withBody(fixture("pr.diff"))));
        github.stubFor(get(urlMatching("/repos/o/r/contents/.+"))
            .willReturn(aResponse().withStatus(404)));   // no .codemate.yml, no pom.xml
        github.stubFor(get(urlMatching("/repos/o/r/contents/Foo.java\\?ref=.+"))
            .willReturn(okJson("""
                {"name":"Foo.java","content":"cHVibGljIGNsYXNzIEZvbyB7IHB1YmxpYyB2b2lkIGJhcigpIHt9IH0K","encoding":"base64"}""")));
        github.stubFor(post(urlEqualTo("/repos/o/r/pulls/1/reviews"))
            .willReturn(okJson("{\"id\":42}")));
        github.stubFor(post(urlMatching("/repos/o/r/statuses/.*"))
            .willReturn(okJson("{}")));

        llm.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson("""
            {"id":"x","object":"chat.completion","created":1234567890,"model":"deepseek-chat",
             "choices":[{"index":0,"message":{"role":"assistant","content":"{\\"items\\":[{\\"line\\":1,\\"title\\":\\"NPE\\",\\"description\\":\\".\\",\\"suggestion\\":\\".\\",\\"suggestedCode\\":\\".\\",\\"severity\\":\\"HIGH\\",\\"confidence\\":90,\\"references\\":[]}]}"},"finish_reason":"stop"}],
             "usage":{"prompt_tokens":100,"completion_tokens":50,"total_tokens":1000}}""")));

        String body = fixture("pr-opened.json");
        String sig = "sha256=" + hmac("topsecret", body);
        HttpHeaders h = new HttpHeaders();
        h.set("X-GitHub-Event", "pull_request");
        h.set("X-Hub-Signature-256", sig);
        h.setContentType(MediaType.APPLICATION_JSON);

        var resp = restTemplate.exchange("/webhook/github", HttpMethod.POST,
            new HttpEntity<>(body, h), Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            github.verify(postRequestedFor(urlEqualTo("/repos/o/r/pulls/1/reviews")));
            assertThat(reviewRepo.findAll())
                .anyMatch(rv -> "success".equals(rv.getStatus()));
        });
    }
}
