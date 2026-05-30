# 📘 项目一：CodeMate Review - AI代码审查机器人

## 一、项目概述

### 1.1 定位

面向Java项目的AI代码审查机器人，自动Review GitHub PR，发现Bug、安全、性能、设计问题。

### 1.2 核心价值

- **AI Review**：5个专业Agent并行分析，覆盖Bug/安全/性能/规范/设计
- **行级评论**：直接在GitHub PR对应代码行发评论 + 修复建议
- **项目感知**：RAG学习项目历史风格，给出"符合本项目"的建议
- **Java深度优化**：内置Spring/JVM/并发的领域规则

### 1.3 与竞品对比

| 维度     | CodeRabbit  | Greptile    | CodeMate Review |
| -------- | ----------- | ----------- | --------------- |
| 价格     | $15/用户/月 | $30/用户/月 | 开源免费        |
| 语言     | Python      | Python      | **Java**        |
| Java深度 | 通用        | 通用        | **专业**        |
| 中文     | ❌           | ❌           | ✅               |
| 自部署   | ❌           | ❌           | ✅               |

---

## 二、功能设计

### 2.1 功能清单

| ID   | 功能               | 优先级 | 说明                          |
| ---- | ------------------ | ------ | ----------------------------- |
| F1   | GitHub Webhook接入 | P0     | 监听PR opened/synchronize事件 |
| F2   | PR Diff解析        | P0     | 提取变更文件/方法/行          |
| F3   | BugAgent           | P0     | NPE/并发/资源泄漏/异常处理    |
| F4   | SecurityAgent      | P0     | SQL注入/XSS/敏感信息/权限     |
| F5   | PerformanceAgent   | P0     | N+1/大对象/低效循环/缓存      |
| F6   | StyleAgent         | P0     | 命名/注释/复杂度/规范         |
| F7   | DesignAgent        | P1     | SOLID/设计模式/架构           |
| F8   | 行级评论发布       | P0     | GitHub Review API             |
| F9   | PR总报告           | P0     | 整体打分+Top问题              |
| F10  | 配置文件           | P0     | `.codemate.yml`               |
| F11  | RAG增强            | P1     | 项目代码索引+检索             |
| F12  | CLI模式            | P1     | 本地运行不依赖GitHub          |
| F13  | Web Dashboard      | P2     | Review历史查看                |
| F14  | GitLab/Gitee支持   | P2     | 多平台                        |
| F15  | IDEA插件           | P2     | IDE实时Review                 |

### 2.2 交互流程

```
开发者push代码到PR
   ↓
GitHub发送Webhook (pull_request.opened/synchronize)
   ↓
CodeMate接收 → 验签 → 入队
   ↓
[异步] 拉取PR Diff + 相关文件
   ↓
JavaParser解析 → 提取"变更方法"列表
   ↓
为每个方法构建上下文（依赖、调用方、项目信息）
   ↓
5个Agent并行Review（虚拟线程）
   ↓
结果聚合（去重、按严重级排序、限制数量）
   ↓
GitHub Review API：
  - 行级评论（高/中严重问题）
  - PR总评（整体报告）
  - 设置PR状态（pass/fail）
```

---

## 三、技术架构

### 3.1 架构图

```
┌────────────────────────────────────────────────────┐
│              GitHub (Webhook源)                     │
└──────────────────────┬─────────────────────────────┘
                       │ HTTPS Webhook
                       ▼
┌────────────────────────────────────────────────────┐
│        Spring Boot Application (JDK 21)            │
│                                                    │
│  [Webhook Layer]                                   │
│  └── 验签 + 事件路由 + 异步入队                      │
│                                                    │
│  [Context Layer]                                   │
│  └── PR解析 + AST分析 + 上下文构建                   │
│                                                    │
│  [Agent Layer] (核心)                              │
│  ├── BugAgent                                      │
│  ├── SecurityAgent                                 │
│  ├── PerformanceAgent                              │
│  ├── StyleAgent                                    │
│  └── DesignAgent                                   │
│        ↓ 并行执行（虚拟线程）                        │
│                                                    │
│  [Aggregator Layer]                                │
│  └── 去重 + 排序 + 限流                             │
│                                                    │
│  [Publisher Layer]                                 │
│  └── GitHub评论发布                                 │
└──────┬───────────────────────────────┬─────────────┘
       ▼                               ▼
┌──────────────┐               ┌──────────────────┐
│  LLM API     │               │  PostgreSQL      │
│ (DeepSeek)   │               │  + pgvector      │
└──────────────┘               │  + Redis         │
                               └──────────────────┘
```

### 3.2 技术栈

| 类别     | 技术                   | 版本     |
| -------- | ---------------------- | -------- |
| 语言     | JDK                    | 21       |
| 框架     | Spring Boot            | 3.3.x    |
| AI框架   | LangChain4j            | 0.36.0   |
| LLM      | DeepSeek-V3 / Qwen-Max | -        |
| 代码解析 | JavaParser             | 3.26.x   |
| GitHub   | kohsuke/github-api     | 1.x      |
| 数据库   | PostgreSQL + pgvector  | 16 + 0.7 |
| 缓存     | Redis                  | 7.x      |
| 构建     | Maven                  | 3.9+     |
| 部署     | Docker + Caddy         | -        |

---

## 四、核心模块设计

### 4.1 Webhook层

**职责**：接收 + 验签 + 异步分发

**关键设计**：

- 同步快速返回200（防止GitHub超时10s）
- 异步队列处理实际Review（用Redis Stream或Spring Event）
- HMAC-SHA256验签
- 同一PR多次push时，**取消上一次正在跑的Review**（防重复消耗LLM）

**接口**：

```
POST /webhook/github
Headers:
  X-GitHub-Event: pull_request
  X-Hub-Signature-256: sha256=xxx
Body: GitHub PR Payload
```

### 4.2 Context层（关键）

**职责**：把PR信息转换成Agent能理解的"上下文"

**核心数据结构**：

```java
PRContext {
    String repoName;
    String prTitle, prDescription;
    String baseSha, headSha;
  
    ProjectInfo projectInfo;      // 项目类型/技术栈/规模
  
    List<ChangedFile> changedFiles;
    List<ChangedMethod> changedMethods;  // ⭐核心：方法级粒度
}

ChangedMethod {
    String filePath;
    String className;
    String methodName;
    String fullCode;              // 完整方法代码
    String diffCode;              // 仅变更部分
  
    ChangeType type;              // ADDED/MODIFIED/DELETED
    List<String> callers;         // 谁调用了这个方法
    List<String> callees;         // 这个方法调了谁
    List<String> dependencies;    // 涉及的外部类
}
```

**为什么用方法级粒度？**

- 文件级太大（Token超限）
- 行级太小（缺上下文）
- 方法级是Java最自然的单元

**JavaParser使用要点**：

- 把PR的Diff行号映射到`MethodDeclaration`
- 用`SymbolSolver`解析方法调用（需要classpath）
- 处理失败时降级到"文件级"Review

### 4.3 Agent层（最核心）

#### Agent基类

```java
public abstract class ReviewAgent {
    public abstract String getName();
    public abstract ReviewCategory getCategory();
    public abstract Set<Severity> getSupportedSeverities();
  
    /**
     * 对单个方法做Review
     */
    public abstract List<ReviewComment> review(
        ChangedMethod method, 
        PRContext context
    );
  
    /**
     * 是否应该跑这个Agent（基于配置）
     */
    public boolean shouldRun(PRContext context) {
        return context.getConfig().isAgentEnabled(getName());
    }
}
```

#### 5个Agent的职责矩阵

| Agent                | 检测维度                        | 关键规则        | 严重级别分布  |
| -------------------- | ------------------------------- | --------------- | ------------- |
| **BugAgent**         | NPE / 并发 / 资源 / 异常 / 边界 | 50+条           | High居多      |
| **SecurityAgent**    | OWASP Top 10 / 敏感信息 / 权限  | OWASP + CWE     | Critical/High |
| **PerformanceAgent** | N+1 / 大对象 / 低效IO / 缓存    | Spring反模式    | Medium        |
| **StyleAgent**       | 命名 / 注释 / 复杂度 / 长方法   | 阿里规约        | Low           |
| **DesignAgent**      | SOLID / 模式 / 耦合             | 项目特定（RAG） | Medium        |

#### Prompt设计原则

**通用模板结构**：

```
[Role]：你是XXX专家
[Goal]：找出XXX类问题
[Rules]：具体规则列表
[Context]：项目背景 + 方法上下文
[Code]：被审查代码
[Output Format]：严格JSON
[Examples]：1-2个少样本
```

**关键决策**：

- **每个Agent独立Prompt**：聚焦单一职责
- **强制JSON输出**：用LangChain4j的`@StructuredOutput`
- **置信度评分**：每个问题LLM自评0-100，过滤低置信
- **少样本学习**：Prompt里塞2-3个高质量案例

#### Agent输出结构

```java
ReviewComment {
    String agentName;              // 哪个Agent发现的
    String filePath;
    int line;                      // 行号（GitHub评论需要）
    Severity severity;             // CRITICAL/HIGH/MEDIUM/LOW
    ReviewCategory category;       // BUG/SECURITY/PERFORMANCE/STYLE/DESIGN
  
    String title;                  // 一句话标题
    String description;            // 详细解释
    String suggestion;             // 修复建议
    String suggestedCode;          // 建议代码（GitHub Suggestion格式）
  
    int confidence;                // 0-100
    List<String> references;       // 参考资料（CWE编号等）
}
```

### 4.4 Aggregator层

**职责**：合并多Agent结果

**逻辑**：

1. **去重**：同一行/同一问题被多个Agent发现 → 合并
2. **排序**：按 `severity × confidence` 降序
3. **限流**：单PR最多评论数（配置，默认20条）
4. **过滤**：confidence < 阈值的丢弃（默认70）
5. **分级**：
   - Critical/High → 行级评论
   - Medium → PR总评里列出
   - Low → 只统计不评论

### 4.5 Publisher层

**职责**：发布到GitHub

**GitHub API用法**：

```
1. 创建Review（POST /repos/{owner}/{repo}/pulls/{pr}/reviews）
   - event: COMMENT / REQUEST_CHANGES / APPROVE
   - comments: 行级评论数组
   - body: 总评内容

2. 提交Status（POST /repos/{owner}/{repo}/statuses/{sha}）
   - state: success/failure/pending
   - context: "codemate-review"
```

**评论格式**（Markdown）：

```markdown
🤖 **CodeMate Review** · [HIGH] · BugAgent

**可能的NullPointerException**

`user.getName()` 在第42行可能抛出NPE，因为前面没有校验 `user` 非空。

💡 **建议**：
```suggestion
if (user == null) {
    throw new UserNotFoundException(userId);
}
return user.getName();
```

<sub>Confidence: 92% · CWE-476 [<sup>1</sup>](https://cwe.mitre.org/data/definitions/476.html)</sub>

```
### 4.6 RAG层（P1）

**用途场景**：
- DesignAgent：检索"项目中类似的现有实现"
- StyleAgent：检索"项目代码规范文档"
- BugAgent：检索"历史Bug记录"

**索引内容**：
- 项目所有Java文件（按类切分）
- README、docs/、CONTRIBUTING
- 历史Issue/PR的描述

**检索策略**：
- 用方法签名 + 描述生成查询向量
- pgvector相似度检索Top 5
- 塞到Agent的Prompt里作为"参考实现"

**索引时机**：
- GitHub App安装时：全量索引
- 每次PR合并后：增量更新

---

## 五、数据模型

### 5.1 主要数据表

```sql
-- 仓库表
CREATE TABLE repositories (
    id BIGSERIAL PRIMARY KEY,
    github_id BIGINT UNIQUE,
    full_name VARCHAR(255),       -- owner/repo
    installation_id BIGINT,        -- GitHub App安装ID
    config JSONB,                  -- .codemate.yml内容
    created_at TIMESTAMP,
    indexed_at TIMESTAMP           -- RAG索引时间
);

-- Review记录
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    repo_id BIGINT REFERENCES repositories(id),
    pr_number INT,
    commit_sha VARCHAR(40),
    status VARCHAR(20),            -- pending/running/success/failed
  
    overall_score INT,             -- 0-100
    critical_count INT,
    high_count INT,
    medium_count INT,
    low_count INT,
  
    llm_tokens_used INT,
    llm_cost_usd DECIMAL(10,4),
    duration_ms INT,
  
    created_at TIMESTAMP,
    finished_at TIMESTAMP
);

-- 具体评论
CREATE TABLE review_comments (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT REFERENCES reviews(id),
    agent_name VARCHAR(50),
    severity VARCHAR(20),
    category VARCHAR(20),
    file_path VARCHAR(500),
    line INT,
    title TEXT,
    description TEXT,
    suggestion TEXT,
    confidence INT,
    github_comment_id BIGINT       -- 同步到GitHub后的ID
);

-- RAG向量索引
CREATE TABLE code_embeddings (
    id BIGSERIAL PRIMARY KEY,
    repo_id BIGINT REFERENCES repositories(id),
    file_path VARCHAR(500),
    method_name VARCHAR(200),
    code_chunk TEXT,
    embedding VECTOR(1536),         -- pgvector
    metadata JSONB,
    indexed_at TIMESTAMP
);

CREATE INDEX ON code_embeddings USING ivfflat (embedding vector_cosine_ops);
```

### 5.2 配置文件设计

`.codemate.yml`（项目根目录）：

```yaml
version: 1

# Agent开关
agents:
  bug: true
  security: true
  performance: true
  style: true
  design: false

# 严重级别阈值（低于此级别不评论）
min_severity: medium

# 单PR最多评论数
max_comments_per_pr: 20

# 置信度阈值
min_confidence: 70

# 排除文件
exclude:
  - "**/generated/**"
  - "**/*.proto"
  - "src/test/**"

# 自定义规则（自然语言）
custom_rules:
  - "禁止在Service层使用System.out"
  - "所有public API必须有JavaDoc"

# LLM配置
llm:
  provider: deepseek
  model: deepseek-chat
  max_tokens_per_review: 50000  # 单PR预算
```

---

## 六、关键设计决策

### 决策1：方法级粒度 vs 文件级

**选方法级**。原因：Java代码以方法为最小语义单元，文件级Token浪费严重。

### 决策2：多Agent vs 单Agent

**选多Agent**。原因：Prompt聚焦、并行加速、独立演进、易于测试。

### 决策3：JSON输出 vs 自由文本

**选严格JSON**（LangChain4j结构化输出）。原因：可解析、可验证、可统计。

### 决策4：实时Review vs 异步Review

**选异步**。原因：LLM调用慢（30s-2min），同步会导致GitHub Webhook超时。

### 决策5：自部署LLM vs API

**选API（DeepSeek/Qwen）**。原因：成本低（DeepSeek $0.27/百万token）、质量高、维护简单。

### 决策6：评论数量控制

**默认每个PR最多20条**。原因：评论太多反而没人看，要"少而精"。

### 决策7：成本控制

- 单PR预算：默认5万Token（约$0.05）
- 超过预算：降级（只跑BugAgent + SecurityAgent）
- 大文件：跳过（>1000行的单文件不Review）

---

## 七、项目结构

```
codemate-review/
├── README.md
├── pom.xml
│
├── core/                                # 核心领域模型
│   └── src/main/java/com/codemate/review/core/
│       ├── model/
│       │   ├── PRContext.java
│       │   ├── ChangedMethod.java
│       │   ├── ReviewComment.java
│       │   └── ReviewResult.java
│       └── enums/
│           ├── Severity.java
│           └── ReviewCategory.java
│
├── parser/                              # 代码解析
│   └── src/main/java/com/codemate/review/parser/
│       ├── JavaCodeParser.java
│       ├── PRDiffParser.java
│       └── MethodExtractor.java
│
├── agent/                               # Agent模块
│   └── src/main/java/com/codemate/review/agent/
│       ├── ReviewAgent.java            # 基类
│       ├── impl/
│       │   ├── BugAgent.java
│       │   ├── SecurityAgent.java
│       │   ├── PerformanceAgent.java
│       │   ├── StyleAgent.java
│       │   └── DesignAgent.java
│       ├── prompt/
│       │   └── PromptTemplates.java
│       └── orchestrator/
│           └── ReviewOrchestrator.java
│
├── aggregator/
│   └── src/main/java/com/codemate/review/aggregator/
│       ├── ResultAggregator.java
│       ├── Deduplicator.java
│       └── Ranker.java
│
├── rag/                                 # RAG模块
│   └── src/main/java/com/codemate/review/rag/
│       ├── indexer/
│       │   └── CodeIndexer.java
│       ├── retriever/
│       │   └── CodeRetriever.java
│       └── embedding/
│           └── EmbeddingService.java
│
├── github/                              # GitHub集成
│   └── src/main/java/com/codemate/review/github/
│       ├── webhook/
│       │   ├── WebhookController.java
│       │   └── SignatureVerifier.java
│       ├── client/
│       │   └── GitHubClient.java
│       └── publisher/
│           └── CommentPublisher.java
│
├── api/                                 # 应用入口
│   └── src/main/java/com/codemate/review/
│       ├── CodeMateApplication.java
│       ├── config/
│       └── controller/
│
├── cli/                                 # CLI工具
│   └── src/main/java/com/codemate/review/cli/
│
└── docker/
    ├── Dockerfile
    └── docker-compose.yml
```

---

