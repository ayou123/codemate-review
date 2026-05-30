package com.codemate.review.config;

import com.codemate.review.aggregator.Deduplicator;
import com.codemate.review.aggregator.Ranker;
import com.codemate.review.aggregator.ResultAggregator;
import com.codemate.review.agent.LlmClient;
import com.codemate.review.agent.LlmRequest;
import com.codemate.review.agent.LlmResponse;
import com.codemate.review.agent.ProjectReferenceProvider;
import com.codemate.review.agent.ReviewAgent;
import com.codemate.review.agent.impl.BugAgent;
import com.codemate.review.agent.impl.DesignAgent;
import com.codemate.review.agent.impl.PerformanceAgent;
import com.codemate.review.agent.impl.SecurityAgent;
import com.codemate.review.agent.impl.StyleAgent;
import com.codemate.review.agent.orchestrator.ReviewOrchestrator;
import com.codemate.review.agent.prompt.PromptTemplates;
import com.codemate.review.core.config.CodemateConfigLoader;
import com.codemate.review.parser.JavaCodeParser;
import com.codemate.review.parser.MethodExtractor;
import com.codemate.review.parser.PRDiffParser;
import com.codemate.review.parser.ProjectInfoDetector;
import com.codemate.review.service.ExclusionFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AgentConfig {

    @Bean
    PRDiffParser prDiffParser() { return new PRDiffParser(); }

    @Bean
    JavaCodeParser javaCodeParser() { return new JavaCodeParser(); }

    @Bean
    MethodExtractor methodExtractor(JavaCodeParser p) { return new MethodExtractor(p); }

    @Bean
    ProjectInfoDetector projectInfoDetector() { return new ProjectInfoDetector(); }

    @Bean
    ExclusionFilter exclusionFilter() { return new ExclusionFilter(); }

    @Bean
    CodemateConfigLoader codemateConfigLoader() { return new CodemateConfigLoader(); }

    @Bean
    PromptTemplates promptTemplates() { return new PromptTemplates(); }

    @Bean
    BugAgent bugAgent(LlmClient l, PromptTemplates t, ProjectReferenceProvider p) { return new BugAgent(l, t, p); }

    @Bean
    SecurityAgent securityAgent(LlmClient l, PromptTemplates t, ProjectReferenceProvider p) { return new SecurityAgent(l, t, p); }

    @Bean
    PerformanceAgent performanceAgent(LlmClient l, PromptTemplates t, ProjectReferenceProvider p) { return new PerformanceAgent(l, t, p); }

    @Bean
    StyleAgent styleAgent(LlmClient l, PromptTemplates t, ProjectReferenceProvider p) { return new StyleAgent(l, t, p); }

    @Bean
    DesignAgent designAgent(LlmClient l, PromptTemplates t, ProjectReferenceProvider p) { return new DesignAgent(l, t, p); }

    @Bean
    ReviewOrchestrator reviewOrchestrator(List<ReviewAgent> agents) {
        return new ReviewOrchestrator(agents);
    }

    @Bean
    Deduplicator deduplicator() { return new Deduplicator(); }

    @Bean
    Ranker ranker() { return new Ranker(); }

    @Bean
    ResultAggregator resultAggregator(Deduplicator d, Ranker r) {
        return new ResultAggregator(d, r);
    }

    /**
     * Placeholder LlmClient used until Task 21 provides a real provider bean
     * (DeepSeek/Qwen). Marked @ConditionalOnMissingBean so Task 21's bean
     * shadows this no-op.
     */
    @Bean
    @ConditionalOnMissingBean(LlmClient.class)
    LlmClient noopLlmClient() {
        return new LlmClient() {
            @Override
            public LlmResponse complete(LlmRequest req) {
                return new LlmResponse("{\"items\":[]}", 0);
            }

            @Override
            public String providerName() {
                return "noop";
            }
        };
    }
}
