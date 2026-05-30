package com.codemate.review.agent.impl;

import com.codemate.review.agent.LlmClient;
import com.codemate.review.agent.ProjectReferenceProvider;
import com.codemate.review.agent.ReviewAgent;
import com.codemate.review.agent.prompt.PromptTemplates;
import com.codemate.review.core.enums.ReviewCategory;

public class DesignAgent extends ReviewAgent {
    public DesignAgent(LlmClient llm, PromptTemplates t) { super(llm, t); }
    public DesignAgent(LlmClient llm, PromptTemplates t, ProjectReferenceProvider refs) { super(llm, t, refs); }

    @Override
    public String getName() { return "design"; }

    @Override
    public ReviewCategory getCategory() { return ReviewCategory.DESIGN; }

    @Override
    protected String systemPrompt() {
        return "你是 Java 架构设计审查专家，专注 SOLID 原则 / 设计模式滥用 / 模块耦合 / 抽象层次。严格输出 JSON，不寒暄。";
    }
}
