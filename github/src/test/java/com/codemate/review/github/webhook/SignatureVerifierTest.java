package com.codemate.review.github.webhook;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureVerifierTest {

    private static String hmac(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] out = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : out) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    @Test
    void validSignaturePasses() throws Exception {
        String secret = "topsecret";
        String body = "{\"hello\":\"world\"}";
        String sig = "sha256=" + hmac(secret, body);
        assertThat(new SignatureVerifier(secret).verify(body, sig)).isTrue();
    }

    @Test
    void invalidSignatureFails() {
        assertThat(new SignatureVerifier("topsecret").verify("body","sha256=deadbeef")).isFalse();
    }

    @Test
    void nullOrMalformedSignatureFails() {
        SignatureVerifier v = new SignatureVerifier("s");
        assertThat(v.verify("body", null)).isFalse();
        assertThat(v.verify("body", "")).isFalse();
        assertThat(v.verify("body", "md5=abc")).isFalse();
        assertThat(v.verify("body", "sha256=")).isFalse();   // empty hex
        assertThat(v.verify("body", "sha256=zzz")).isFalse(); // bad hex
    }
}
