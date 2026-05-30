package com.codemate.review.config;

import com.codemate.review.agent.ProjectReferenceProvider;
import com.codemate.review.rag.retriever.CodeRetriever;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.stream.Collectors;

@Configuration
public class RagAgentConfig {

    @Bean
    @ConditionalOnBean(CodeRetriever.class)
    @Primary
    ProjectReferenceProvider ragProjectReferenceProvider(CodeRetriever retriever) {
        return (method, ctx) -> {
            if (ctx == null || ctx.repoId() == null) return "";
            String query = method.className() + "." + method.methodName() + " " + method.fullCode();
            var sims = retriever.findSimilar(ctx.repoId(), query, 3);
            if (sims.isEmpty()) return "";
            return sims.stream()
                .map(s -> "// " + s.filePath() + " (" + s.methodName() + ")\n" + s.codeChunk())
                .collect(Collectors.joining("\n\n", "[Project References]\n", ""));
        };
    }

    @Bean
    @ConditionalOnMissingBean(ProjectReferenceProvider.class)
    ProjectReferenceProvider noopProvider() {
        return (m, c) -> "";
    }
}
