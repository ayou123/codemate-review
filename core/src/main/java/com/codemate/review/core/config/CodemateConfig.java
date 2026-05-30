package com.codemate.review.core.config;

import com.codemate.review.core.enums.Severity;

import java.util.List;

public record CodemateConfig(
        int version,
        Agents agents,
        Severity minSeverity,
        int maxCommentsPerPr,
        int minConfidence,
        List<String> exclude,
        List<String> customRules,
        Llm llm
) {
    public CodemateConfig {
        exclude = exclude == null ? List.of() : List.copyOf(exclude);
        customRules = customRules == null ? List.of() : List.copyOf(customRules);
    }

    public record Agents(boolean bug, boolean security, boolean performance, boolean style, boolean design) {}

    public record Llm(String provider, String model, int maxTokensPerReview) {}

    public static CodemateConfig defaults() {
        return new CodemateConfig(
                1,
                new Agents(true, true, true, true, false),
                Severity.MEDIUM,
                20,
                70,
                List.of(),
                List.of(),
                new Llm("deepseek", "deepseek-chat", 50_000)
        );
    }

    public boolean isAgentEnabled(String agentName) {
        return switch (agentName) {
            case "bug" -> agents.bug();
            case "security" -> agents.security();
            case "performance" -> agents.performance();
            case "style" -> agents.style();
            case "design" -> agents.design();
            default -> false;
        };
    }
}
