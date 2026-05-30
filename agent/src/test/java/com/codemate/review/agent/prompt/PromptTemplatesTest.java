package com.codemate.review.agent.prompt;
import com.codemate.review.core.config.CodemateConfig;
import com.codemate.review.core.model.ChangedMethod;
import com.codemate.review.core.model.PRContext;
import com.codemate.review.core.model.ProjectInfo;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplatesTest {
    @Test
    void rendersTemplateWithMethodAndProjectContext() {
        var t = new PromptTemplates();
        var ctx = PRContext.builder()
            .repoName("o/r")
            .projectInfo(ProjectInfo.builder()
                .buildTool("maven")
                .frameworks(List.of("spring-boot"))
                .build())
            .config(CodemateConfig.defaults())
            .build();
        var m = ChangedMethod.builder()
            .filePath("Foo.java").className("Foo")
            .methodName("bar").fullCode("public void bar(){}")
            .diffCode("+    public void bar(){}")
            .build();
        String prompt = t.render("_test-agent", m, ctx, "");
        assertThat(prompt).contains("Foo.java");
        assertThat(prompt).contains("public void bar(){}");
        assertThat(prompt).contains("spring-boot");
        assertThat(prompt).contains("[Output Format]");
    }

    @Test
    void unknownTemplateReturnsEmptyString() {
        // Defensive: missing template files don't crash the bot.
        assertThat(new PromptTemplates().render("no-such-agent",
            ChangedMethod.builder().methodName("x").build(),
            PRContext.builder().config(CodemateConfig.defaults()).build(),
            "")).isEmpty();
    }
}
