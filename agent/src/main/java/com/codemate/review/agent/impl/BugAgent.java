package com.codemate.review.agent.impl;

import com.codemate.review.agent.LlmClient;
import com.codemate.review.agent.ProjectReferenceProvider;
import com.codemate.review.agent.ReviewAgent;
import com.codemate.review.agent.prompt.PromptTemplates;
import com.codemate.review.core.enums.ReviewCategory;

public class BugAgent extends ReviewAgent {
    public BugAgent(LlmClient llm, PromptTemplates t) { super(llm, t); }
    public BugAgent(LlmClient llm, PromptTemplates t, ProjectReferenceProvider refs) { super(llm, t, refs); }

    @Override
    public String getName() { return "bug"; }

    @Override
    public ReviewCategory getCategory() { return ReviewCategory.BUG; }

    @Override
    protected String systemPrompt() {
        return "你是资深 Java Bug 审查专家，专注 NPE / 并发 / 资源泄漏 / 异常处理 / 边界条件等运行期缺陷。严格输出 JSON，不寒暄。";
    }
}
