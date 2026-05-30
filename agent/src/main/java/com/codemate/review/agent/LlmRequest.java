package com.codemate.review.agent;

public record LlmRequest(String systemPrompt, String userPrompt, double temperature, int maxTokens) {
}
