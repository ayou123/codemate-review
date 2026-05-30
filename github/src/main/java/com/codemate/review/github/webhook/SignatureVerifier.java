package com.codemate.review.github.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class SignatureVerifier {
    private final byte[] secret;

    public SignatureVerifier(String secret) {
        this.secret = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
    }

    public boolean verify(String body, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) return false;
        String hex = signatureHeader.substring("sha256=".length());
        if (hex.isEmpty()) return false;
        byte[] provided;
        try { provided = hexDecode(hex); }
        catch (IllegalArgumentException e) { return false; }
        if (provided.length != 32) return false;  // SHA-256 is 32 bytes
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] expected = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(expected, provided);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] hexDecode(String hex) {
        if ((hex.length() & 1) != 0) throw new IllegalArgumentException("odd hex");
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i*2), 16);
            int lo = Character.digit(hex.charAt(i*2+1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("bad hex");
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
