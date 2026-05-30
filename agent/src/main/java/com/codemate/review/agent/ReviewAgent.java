package com.codemate.review.agent;

import com.codemate.review.agent.prompt.PromptTemplates;
import com.codemate.review.core.enums.ReviewCategory;
import com.codemate.review.core.enums.Severity;
import com.codemate.review.core.model.ChangedMethod;
import com.codemate.review.core.model.PRContext;
import com.codemate.review.core.model.ReviewComment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class ReviewAgent {
    private static final Logger log = LoggerFactory.getLogger(ReviewAgent.class);

    protected final LlmClient llm;
    protected final PromptTemplates templates;
    protected final ProjectReferenceProvider refs;
    protected final ObjectMapper json = new ObjectMapper();

    protected ReviewAgent(LlmClient llm, PromptTemplates t) {
        this(llm, t, (m, c) -> "");
    }

    protected ReviewAgent(LlmClient llm, PromptTemplates t, ProjectReferenceProvider refs) {
        this.llm = llm;
        this.templates = t;
        this.refs = refs;
    }

    public abstract String getName();
    public abstract ReviewCategory getCategory();
    protected abstract String systemPrompt();

    public boolean shouldRun(PRContext ctx) {
        return ctx.config() != null && ctx.config().isAgentEnabled(getName());
    }

    public List<ReviewComment> review(ChangedMethod method, PRContext ctx) {
        return reviewWithUsage(method, ctx).comments();
    }

    public ReviewBatch reviewWithUsage(ChangedMethod method, PRContext ctx) {
        String references = refs.referencesFor(method, ctx);
        String userPrompt = templates.render(getName(), method, ctx, references);
        if (userPrompt.isEmpty()) {
            log.warn("agent {}: empty prompt — template missing?", getName());
            return ReviewBatch.empty();
        }
        LlmResponse resp = llm.complete(new LlmRequest(systemPrompt(), userPrompt, 0.2, 4000));
        List<ReviewComment> comments = parseComments(resp.content(), method);
        return new ReviewBatch(comments, resp.tokensUsed());
    }

    protected List<ReviewComment> parseComments(String content, ChangedMethod m) {
        if (content == null || content.isBlank()) return List.of();
        try {
            JsonNode root = json.readTree(content);
            JsonNode items = root.path("items");
            if (!items.isArray()) return List.of();
            List<ReviewComment> out = new ArrayList<>();
            for (JsonNode it : items) {
                int line = it.path("line").asInt(m.startLine());
                String sev = it.path("severity").asText("MEDIUM");
                Severity severity;
                try { severity = Severity.fromConfig(sev); }
                catch (RuntimeException ex) { severity = Severity.MEDIUM; }
                out.add(ReviewComment.builder()
                    .agentName(getName())
                    .category(getCategory())
                    .filePath(m.filePath())
                    .line(line)
                    .severity(severity)
                    .title(it.path("title").asText(""))
                    .description(it.path("description").asText(""))
                    .suggestion(it.path("suggestion").asText(""))
                    .suggestedCode(it.path("suggestedCode").asText(""))
                    .confidence(it.path("confidence").asInt(50))
                    .references(toStringList(it.path("references")))
                    .build());
            }
            return out;
        } catch (Exception e) {
            log.warn("agent {} failed to parse LLM output: {}", getName(), e.getMessage());
            return List.of();
        }
    }

    private static List<String> toStringList(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> out = new ArrayList<>();
        node.forEach(n -> out.add(n.asText()));
        return out;
    }
}
