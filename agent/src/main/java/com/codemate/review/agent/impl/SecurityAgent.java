package com.codemate.review.agent.impl;

import com.codemate.review.agent.LlmClient;
import com.codemate.review.agent.ProjectReferenceProvider;
import com.codemate.review.agent.ReviewAgent;
import com.codemate.review.agent.prompt.PromptTemplates;
import com.codemate.review.core.enums.ReviewCategory;

public class SecurityAgent extends ReviewAgent {
    public SecurityAgent(LlmClient llm, PromptTemplates t) { super(llm, t); }
    public SecurityAgent(LlmClient llm, PromptTemplates t, ProjectReferenceProvider refs) { super(llm, t, refs); }

    @Override
    public String getName() { return "security"; }

    @Override
    public ReviewCategory getCategory() { return ReviewCategory.SECURITY; }

    @Override
    protected String systemPrompt() {
        return "你是 Java 安全审查专家，专注 OWASP Top 10 / CWE 常见漏洞 / 敏感信息泄漏 / 权限校验。严格输出 JSON，不寒暄。";
    }
}
