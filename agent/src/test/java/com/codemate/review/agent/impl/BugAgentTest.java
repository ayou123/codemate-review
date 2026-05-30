package com.codemate.review.agent.impl;

import com.codemate.review.agent.LlmClient;
import com.codemate.review.agent.LlmRequest;
import com.codemate.review.agent.LlmResponse;
import com.codemate.review.agent.prompt.PromptTemplates;
import com.codemate.review.core.config.CodemateConfig;
import com.codemate.review.core.enums.ReviewCategory;
import com.codemate.review.core.enums.Severity;
import com.codemate.review.core.model.ChangedMethod;
import com.codemate.review.core.model.PRContext;
import com.codemate.review.core.model.ProjectInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BugAgentTest {

    private final LlmClient llm = mock(LlmClient.class);
    private final PromptTemplates t = new PromptTemplates();

    private PRContext sampleCtx(CodemateConfig cfg) {
        return PRContext.builder()
                .repoName("o/r")
                .projectInfo(ProjectInfo.builder().buildTool("maven").frameworks(List.of("spring-boot")).build())
                .config(cfg)
                .build();
    }

    private ChangedMethod sampleMethod() {
        return ChangedMethod.builder()
                .filePath("Foo.java").className("Foo")
                .methodName("bar").fullCode("public void bar(){}")
                .diffCode("+ public void bar(){}")
                .startLine(10).endLine(12)
                .build();
    }

    @Test
    void returnsCommentsFromLlmJson() {
        when(llm.complete(any(LlmRequest.class))).thenReturn(new LlmResponse("""
                {"items":[{"line":42,"title":"NPE","description":"...","suggestion":"guard","suggestedCode":"if(u==null)...","severity":"HIGH","confidence":92,"references":["CWE-476"]}]}""", 1500));
        var out = new BugAgent(llm, t).review(sampleMethod(), sampleCtx(CodemateConfig.defaults()));
        assertThat(out).hasSize(1);
        assertThat(out.get(0).agentName()).isEqualTo("bug");
        assertThat(out.get(0).category()).isEqualTo(ReviewCategory.BUG);
        assertThat(out.get(0).severity()).isEqualTo(Severity.HIGH);
        assertThat(out.get(0).line()).isEqualTo(42);
    }

    @Test
    void invalidJsonReturnsEmpty() {
        when(llm.complete(any(LlmRequest.class))).thenReturn(new LlmResponse("not json", 100));
        assertThat(new BugAgent(llm, t).review(sampleMethod(), sampleCtx(CodemateConfig.defaults()))).isEmpty();
    }

    @Test
    void shouldRunHonorsConfig() {
        var defaults = CodemateConfig.defaults();
        var disabledBug = new CodemateConfig(
                defaults.version(),
                new CodemateConfig.Agents(false, defaults.agents().security(),
                        defaults.agents().performance(), defaults.agents().style(),
                        defaults.agents().design()),
                defaults.minSeverity(), defaults.maxCommentsPerPr(), defaults.minConfidence(),
                defaults.exclude(), defaults.customRules(), defaults.llm());
        assertThat(new BugAgent(llm, t).shouldRun(sampleCtx(disabledBug))).isFalse();
        assertThat(new BugAgent(llm, t).shouldRun(sampleCtx(defaults))).isTrue();
    }
}
