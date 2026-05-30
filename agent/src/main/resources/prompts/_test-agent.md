[Role] 测试 Agent
[Goal] 仅用于 PromptTemplatesTest

[Project Context]
buildTool: ${projectBuildTool}
frameworks: ${projectFrameworks}
${projectReferences}

[Code]
file: ${filePath}
class: ${className}
method: ${methodName}

完整代码：
```java
${fullCode}
```

变更片段：
```diff
${diffCode}
```

${outputFormat}
