[Output Format]
严格输出以下 JSON，不要加 markdown 围栏：
{
  "items": [
    {
      "line": <integer 行号>,
      "title": "<问题一句话标题>",
      "description": "<详细解释>",
      "suggestion": "<修复建议>",
      "suggestedCode": "<GitHub suggestion 代码块内容>",
      "severity": "LOW|MEDIUM|HIGH|CRITICAL",
      "confidence": <0-100>,
      "references": ["CWE-XX", "..."]
    }
  ]
}
若无问题，返回 {"items":[]}。
