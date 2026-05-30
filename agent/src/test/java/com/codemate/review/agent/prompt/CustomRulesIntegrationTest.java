package com.codemate.review.agent.prompt;

import com.codemate.review.core.config.CodemateConfig;
import com.codemate.review.core.model.ChangedMethod;
import com.codemate.review.core.model.PRContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomRulesIntegrationTest {
    @Test
    void customRulesAreRenderedIntoBugPrompt() {
        var defaults = CodemateConfig.defaults();
        var cfg = new CodemateConfig(defaults.version(), defaults.agents(), defaults.minSeverity(),
            defaults.maxCommentsPerPr(), defaults.minConfidence(), defaults.exclude(),
            List.of("禁止在 Service 层使用 System.out", "所有 public API 必须有 JavaDoc"),
            defaults.llm());

        var ctx = PRContext.builder()
            .repoName("o/r").config(cfg).build();
        var m = ChangedMethod.builder()
            .filePath("X.java").className("X").methodName("y").fullCode("public void y(){}").build();

        String prompt = new PromptTemplates().render("bug", m, ctx, "");
        assertThat(prompt).contains("禁止在 Service 层使用 System.out");
        assertThat(prompt).contains("所有 public API 必须有 JavaDoc");
    }

    @Test
    void emptyCustomRulesProducesNoErrors() {
        var ctx = PRContext.builder().config(CodemateConfig.defaults()).build();
        var m = ChangedMethod.builder().methodName("y").fullCode("...").build();
        String prompt = new PromptTemplates().render("bug", m, ctx, "");
        // Should at least contain the role
        assertThat(prompt).isNotEmpty();
    }
}
