package com.codemate.review.agent;

public interface LlmClient {

    LlmResponse complete(LlmRequest req);

    String providerName();
}
