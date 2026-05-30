package com.codemate.review.core.config;

import com.codemate.review.core.enums.Severity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodemateConfigLoaderTest {

    @Test
    void loadsFullYaml() {
        var cfg = new CodemateConfigLoader().loadFromClasspath("/sample-codemate.yml");
        assertThat(cfg.agents().bug()).isTrue();
        assertThat(cfg.agents().design()).isFalse();
        assertThat(cfg.minSeverity()).isEqualTo(Severity.MEDIUM);
        assertThat(cfg.maxCommentsPerPr()).isEqualTo(20);
        assertThat(cfg.minConfidence()).isEqualTo(70);
        assertThat(cfg.exclude()).contains("**/generated/**", "**/*.proto", "src/test/**");
        assertThat(cfg.customRules()).hasSize(2);
        assertThat(cfg.llm().provider()).isEqualTo("deepseek");
        assertThat(cfg.llm().maxTokensPerReview()).isEqualTo(50_000);
    }

    @Test
    void missingFileReturnsDefault() {
        var cfg = new CodemateConfigLoader().loadFromString("");
        assertThat(cfg.agents().bug()).isTrue();
        assertThat(cfg.maxCommentsPerPr()).isEqualTo(20);
    }

    @Test
    void partialYmlOverridesOnlySpecifiedFields() {
        var cfg = new CodemateConfigLoader().loadFromString("min_confidence: 50");
        assertThat(cfg.minConfidence()).isEqualTo(50);
        assertThat(cfg.maxCommentsPerPr()).isEqualTo(20);
    }
}
