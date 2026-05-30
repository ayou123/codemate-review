package com.codemate.review.github.webhook;

import com.codemate.review.core.queue.ReviewJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = WebhookControllerTest.TestConfig.class)
@AutoConfigureMockMvc
class WebhookControllerTest {

    @MockBean WebhookDispatcher dispatcher;
    @Autowired MockMvc mvc;

    @SpringBootConfiguration
    static class TestConfig {
        @Bean SignatureVerifier signatureVerifier() { return new SignatureVerifier("testsecret"); }
        @Bean WebhookPayloadParser webhookPayloadParser() { return new WebhookPayloadParser(); }
        @Bean WebhookController webhookController(SignatureVerifier v, WebhookDispatcher d, WebhookPayloadParser p) {
            return new WebhookController(v, d, p);
        }
    }

    private static String hmac(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] out = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : out) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String fixture(String name) throws Exception {
        return Files.readString(Path.of("src/test/resources/fixtures/" + name));
    }

    @Test
    void returns202OnValidPROpened() throws Exception {
        String body = fixture("pr-opened.json");
        String sig = "sha256=" + hmac("testsecret", body);
        mvc.perform(post("/webhook/github")
                .header("X-GitHub-Event", "pull_request")
                .header("X-Hub-Signature-256", sig)
                .contentType("application/json").content(body))
           .andExpect(status().isAccepted());
        verify(dispatcher).enqueue(any(ReviewJob.class));
    }

    @Test
    void returns401OnBadSignature() throws Exception {
        mvc.perform(post("/webhook/github")
                .header("X-GitHub-Event", "pull_request")
                .header("X-Hub-Signature-256", "sha256=bad")
                .contentType("application/json").content("{}"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void ignoresNonPREvents() throws Exception {
        String body = "{}";
        String sig = "sha256=" + hmac("testsecret", body);
        mvc.perform(post("/webhook/github")
                .header("X-GitHub-Event", "push")
                .header("X-Hub-Signature-256", sig)
                .contentType("application/json").content(body))
           .andExpect(status().isNoContent());
        verifyNoInteractions(dispatcher);
    }

    @Test
    void ignoresClosedAction() throws Exception {
        String body = fixture("pr-opened.json").replace("\"opened\"", "\"closed\"");
        String sig = "sha256=" + hmac("testsecret", body);
        mvc.perform(post("/webhook/github")
                .header("X-GitHub-Event", "pull_request")
                .header("X-Hub-Signature-256", sig)
                .contentType("application/json").content(body))
           .andExpect(status().isNoContent());
        verifyNoInteractions(dispatcher);
    }
}
