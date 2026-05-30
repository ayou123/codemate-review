package com.codemate.review.agent.impl;

import com.codemate.review.agent.LlmClient;
import com.codemate.review.agent.ProjectReferenceProvider;
import com.codemate.review.agent.ReviewAgent;
import com.codemate.review.agent.prompt.PromptTemplates;
import com.codemate.review.core.enums.ReviewCategory;

public class StyleAgent extends ReviewAgent {
    public StyleAgent(LlmClient llm, PromptTemplates t) { super(llm, t); }
    public StyleAgent(LlmClient llm, PromptTemplates t, ProjectReferenceProvider refs) { super(llm, t, refs); }

    @Override
    public String getName() { return "style"; }

    @Override
    public ReviewCategory getCategory() { return ReviewCategory.STYLE; }

    @Override
    protected String systemPrompt() {
        return "你是 Java 代码规范审查专家，遵循阿里巴巴 Java 开发手册，关注命名 / 注释 / 复杂度 / 长方法 / 魔法值。严格输出 JSON，不寒暄。";
    }
}
