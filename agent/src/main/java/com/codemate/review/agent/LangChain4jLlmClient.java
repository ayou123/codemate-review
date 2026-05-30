package com.codemate.review.agent;

import java.time.Duration;
import java.util.List;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class LangChain4jLlmClient implements LlmClient {

    private final ChatLanguageModel model;
    private final String provider;

    private LangChain4jLlmClient(ChatLanguageModel model, String provider) {
        this.model = model;
        this.provider = provider;
    }

    public static LangChain4jLlmClient deepseek(String apiKey, String baseUrl, String modelName) {
        // DeepSeek is OpenAI-compatible. LangChain4j's OpenAiChatModel derives
        // ${baseUrl}/chat/completions, so the baseUrl must include /v1.
        String fullBaseUrl;
        if (baseUrl.endsWith("/v1") || baseUrl.endsWith("/v1/")) {
            fullBaseUrl = baseUrl;
        } else if (baseUrl.endsWith("/")) {
            fullBaseUrl = baseUrl + "v1";
        } else {
            fullBaseUrl = baseUrl + "/v1";
        }
        OpenAiChatModel llm = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(fullBaseUrl)
                .modelName(modelName)
                .responseFormat("json_object")
                .temperature(0.2)
                .timeout(Duration.ofSeconds(120))
                .maxRetries(1)
                .build();
        return new LangChain4jLlmClient(llm, "deepseek");
    }

    public static LangChain4jLlmClient qwen(String apiKey, String modelName) {
        QwenChatModel llm = QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
        return new LangChain4jLlmClient(llm, "qwen");
    }

    @Override
    public LlmResponse complete(LlmRequest req) {
        List<ChatMessage> msgs = List.of(
                SystemMessage.from(req.systemPrompt()),
                UserMessage.from(req.userPrompt()));
        var resp = model.generate(msgs);
        int tokens = (resp.tokenUsage() == null || resp.tokenUsage().totalTokenCount() == null)
                ? 0
                : resp.tokenUsage().totalTokenCount();
        return new LlmResponse(resp.content().text(), tokens);
    }

    @Override
    public String providerName() {
        return provider;
    }
}
