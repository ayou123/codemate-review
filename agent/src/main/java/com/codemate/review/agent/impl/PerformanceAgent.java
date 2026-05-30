package com.codemate.review.agent.impl;

import com.codemate.review.agent.LlmClient;
import com.codemate.review.agent.ProjectReferenceProvider;
import com.codemate.review.agent.ReviewAgent;
import com.codemate.review.agent.prompt.PromptTemplates;
import com.codemate.review.core.enums.ReviewCategory;

public class PerformanceAgent extends ReviewAgent {
    public PerformanceAgent(LlmClient llm, PromptTemplates t) { super(llm, t); }
    public PerformanceAgent(LlmClient llm, PromptTemplates t, ProjectReferenceProvider refs) { super(llm, t, refs); }

    @Override
    public String getName() { return "performance"; }

    @Override
    public ReviewCategory getCategory() { return ReviewCategory.PERFORMANCE; }

    @Override
    protected String systemPrompt() {
        return "你是 Java 性能专家，专注 N+1 查询 / 内存大对象 / 低效 IO / 缓存缺失 / 算法复杂度。严格输出 JSON，不寒暄。";
    }
}
