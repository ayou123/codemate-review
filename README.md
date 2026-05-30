# CodeMate Review

## 项目简介

CodeMate Review 是一个面向 Java PR 的 AI 代码评审机器人。它通过 5 个专精 Agent（Bug、Security、Performance、Style、Design）并行评审，结合方法级 AST 解析与可选的 RAG 项目上下文检索，自动在 GitHub PR 中发布精准的行级评论。

## 架构图

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

## 技术栈

- JDK 21（虚拟线程）
- Spring Boot 3.3
- LangChain4j 0.36
- JavaParser 3.26
- kohsuke/github-api 1.x
- PostgreSQL 16 + pgvector 0.7
- Redis 7
- Maven 3.9

## 快速启动

使用 Docker Compose 一键启动（包含 PostgreSQL、Redis、应用本体）：

```bash
cp .env.example .env
# 编辑 .env，填入 DEEPSEEK_API_KEY、GITHUB_TOKEN、GITHUB_WEBHOOK_SECRET
docker compose up -d
curl localhost:8080/actuator/health
```

## 环境变量

| 变量名                         | 说明                                              | 默认值                                 |
| ------------------------------ | ------------------------------------------------- | -------------------------------------- |
| `LLM_PROVIDER`                 | LLM 提供方：`deepseek` 或 `qwen`                  | `deepseek`                             |
| `DEEPSEEK_API_KEY`             | DeepSeek API Key（当 provider=deepseek）          | -                                      |
| `DASHSCOPE_API_KEY`            | 阿里云 DashScope API Key（当 provider=qwen）      | -                                      |
| `GITHUB_TOKEN`                 | GitHub PAT，用于读 PR / 发评论                    | -                                      |
| `GITHUB_WEBHOOK_SECRET`        | Webhook HMAC-SHA256 签名密钥                      | -                                      |
| `DB_URL`                       | JDBC URL                                          | `jdbc:postgresql://postgres:5432/codemate` |
| `DB_USER`                      | 数据库用户名                                      | `codemate`                             |
| `DB_PASSWORD`                  | 数据库密码                                        | `codemate`                             |
| `REDIS_HOST`                   | Redis 主机                                        | `redis`                                |
| `REDIS_PORT`                   | Redis 端口                                        | `6379`                                 |
| `CODEMATE_QUEUE_ENABLED`       | 启用 Redis Stream 异步队列                        | `true`                                 |
| `CODEMATE_RAG_ENABLED`         | 启用 RAG 项目上下文检索                           | `false`                                |

## GitHub Webhook 配置

1. 进入 GitHub 仓库 **Settings → Webhooks → Add webhook**
2. **Payload URL**：`https://your-domain/webhook/github`
3. **Content type**：`application/json`
4. **Secret**：与 `.env` 中 `GITHUB_WEBHOOK_SECRET` 一致
5. **Which events**：选择 *Just the pull request events*（Pull requests）
6. 保存后，GitHub 会发一次 ping，应返回 200

## 配置文件 .codemate.yml

将 `.codemate.yml` 放在被评审仓库的根目录，可按需调整：

| 字段                   | 说明                                                                    |
| ---------------------- | ----------------------------------------------------------------------- |
| `version`              | 配置文件 schema 版本，固定 `1`                                          |
| `agents.*`             | 单独开关 5 个 Agent（bug / security / performance / style / design）    |
| `min_severity`         | 评论最低严重度：`low` / `medium` / `high` / `critical`，低于此级别丢弃  |
| `max_comments_per_pr`  | 单 PR 最大评论数，超出后按置信度排序截断                                |
| `min_confidence`       | 评论最低置信度（0-100），低于此值丢弃                                   |
| `exclude`              | glob 排除模式数组（生成代码、proto、测试目录等）                        |
| `custom_rules`         | 自定义自然语言规则数组，注入到所有 Agent 的 Prompt(可以添加代码审查规范)                      |
| `llm.provider`         | LLM 提供方：`deepseek` / `qwen`                                         |
| `llm.model`            | 具体模型名，如 `deepseek-chat`、`qwen-max`                              |
| `llm.max_tokens_per_review` | 单 PR LLM token 预算上限                                           |

仓库根目录的 `.codemate.yml` 是项目示例文件，可直接复制到目标仓库。

## 本地开发

```bash
# 编译（跳过测试）
mvn clean package -DskipTests

# 单元测试
mvn test

# 集成测试（需要 Docker，覆盖父 POM 的 docker 排除）
mvn test -DexcludedGroups=

# 仅运行 api 模块
mvn -pl api spring-boot:run
```

## 模块结构

- **core** — 核心领域模型（PRContext、ChangedMethod、ReviewComment、ReviewResult 与枚举）
- **parser** — Java 代码与 PR Diff 解析（JavaParser），方法级提取
- **agent** — 5 个 Agent 实现 + Prompt 模板 + Orchestrator 并行编排
- **aggregator** — 评审结果去重、排序、限流
- **rag** — pgvector 嵌入索引与检索（可选）
- **github** — Webhook 验签、GitHub API 客户端、PR 评论发布
- **api** — Spring Boot 应用入口、控制器、配置装配

## 状态

MVP — P0 完成，包含 RAG。未实现：CLI 模式、Web Dashboard、GitLab/Gitee、IDEA 插件、GitHub App JWT 安装 token 兑换（当前用 PAT）。

## License

TBD — see LICENSE
