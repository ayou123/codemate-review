# CodeMate Review

CodeMate Review 是一个面向 Java 项目的 AI Pull Request 代码评审机器人。它接收 GitHub PR Webhook，解析 PR diff 和 Java AST 上下文，调度多个专精 Agent 并行审查代码，最后把高置信度问题以 GitHub Review Comment 的形式发布到 PR 对应行。

项目当前处于 MVP 阶段，适合作为 AI 代码评审、LangChain4j 工程化、多 Agent 编排、GitHub Webhook 集成、RAG 检索增强等方向的学习和二次开发基础。

## 核心能力

- **GitHub PR 自动评审**：监听 `pull_request` 事件，自动拉取 PR diff、文件内容并发布行级评论。
- **多 Agent 并行审查**：内置 Bug、Security、Performance、Style、Design 5 类 Agent，覆盖缺陷、安全、性能、规范和设计问题。
- **Java 方法级上下文解析**：基于 JavaParser 解析 AST，将 diff 映射到方法级变更，减少大模型无关上下文输入。
- **结果聚合与限流**：对多 Agent 输出进行去重、排序、置信度过滤和单 PR 评论数控制。
- **可选 RAG 项目上下文**：通过 PostgreSQL + pgvector 建立代码片段索引，为评审提供项目级参考。
- **仓库级配置**：目标仓库可通过 `.codemate.yml` 开关 Agent、设置严重级别阈值、排除文件和注入自定义审查规则。
- **异步任务处理**：使用 Redis Stream 解耦 Webhook 接收和评审执行，避免 GitHub Webhook 请求阻塞。

## 工作流程

```text
GitHub Pull Request
        |
        v
POST /webhook/github
        |
        v
Webhook 验签与事件解析
        |
        v
Redis Stream 异步入队
        |
        v
拉取 PR diff / 文件内容 / 仓库配置
        |
        v
JavaParser 解析变更文件与方法上下文
        |
        v
Bug / Security / Performance / Style / Design Agent 并行评审
        |
        v
去重、排序、阈值过滤、评论数限制
        |
        v
发布 GitHub PR Review Comment
```

## 技术栈

| 方向 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot 3.3 |
| 语言与构建 | JDK 21、Maven 3.9 |
| LLM 编排 | LangChain4j 0.36 |
| 代码解析 | JavaParser 3.26 |
| GitHub 集成 | kohsuke/github-api、GitHub Webhook |
| 数据库 | PostgreSQL 16、Flyway |
| 向量检索 | pgvector |
| 异步队列 | Redis 7、Redis Stream |
| 测试 | JUnit 5、Testcontainers、WireMock、Awaitility |

## 模块结构

```text
.
├── core        # 领域模型、枚举、仓库级配置加载
├── parser      # PR diff 解析、Java AST 解析、方法级上下文提取
├── agent       # LLM Client、Prompt 模板、5 类 Review Agent、编排器
├── aggregator  # 评论去重、排序、阈值过滤、限流
├── rag         # 代码向量化、pgvector 索引与检索
├── github      # GitHub Webhook、验签、PR API 客户端、评论发布
└── api         # Spring Boot 应用入口、配置、持久化、队列消费
```

## 快速开始

### 1. 准备依赖

本项目需要：

- JDK 21+
- Maven 3.8+
- Docker / Docker Compose
- 一个可用的 LLM API Key
- 一个有目标仓库权限的 GitHub Personal Access Token

### 2. 配置环境变量

复制环境变量模板：

```bash
cp .env.example .env
```

编辑 `.env`，至少填写：

```env
LLM_PROVIDER=deepseek
DEEPSEEK_API_KEY=sk-your-api-key
GITHUB_TOKEN=github_pat_xxx
GITHUB_WEBHOOK_SECRET=change-me-to-a-random-secret
```

如果你使用 OpenAI 兼容网关或第三方模型服务，可以继续配置：

```env
CODEMATE_LLM_DEEPSEEK_BASE_URL=https://api.your-llm-provider.com
CODEMATE_LLM_DEEPSEEK_MODEL=gpt-4o-mini
```

说明：当前代码中 `deepseek` 分支实际使用的是 OpenAI Chat Completions 兼容客户端，因此也可以接入支持 `/chat/completions` 的兼容端点。`base-url` 不需要带 `/v1`，客户端会自动处理。

### 3. Docker Compose 启动

```bash
docker compose up -d --build
curl http://localhost:8080/actuator/health
```

期望返回：

```json
{"status":"UP"}
```

默认 Compose 会启动：

- `postgres`：PostgreSQL 16 + pgvector
- `redis`：Redis 7
- `app`：CodeMate Review Spring Boot 应用

### 4. 配置 GitHub Webhook

进入目标 GitHub 仓库：

1. 打开 **Settings** -> **Webhooks** -> **Add webhook**
2. `Payload URL` 填写：`https://your-domain/webhook/github`
3. `Content type` 选择：`application/json`
4. `Secret` 填写：与 `.env` 中 `GITHUB_WEBHOOK_SECRET` 一致
5. `Which events would you like to trigger this webhook?` 选择 **Let me select individual events**，勾选 **Pull requests**
6. 保存后，GitHub 会发送一次 `ping` 事件；服务对非 PR 事件返回 `204 No Content` 属于正常现象

本地调试时可以用 ngrok 暴露服务：

```bash
ngrok http 8080
```

然后将 ngrok 分配的 HTTPS 地址配置为：

```text
https://xxxx.ngrok-free.app/webhook/github
```

## GitHub Token 权限

推荐使用 Fine-grained Personal Access Token，并只授权需要评审的仓库。

最小权限建议：

| 权限 | 级别 | 用途 |
| --- | --- | --- |
| Contents | Read-only | 读取 PR 变更文件内容 |
| Pull requests | Read and write | 读取 PR 并发布 review 评论 |
| Commit statuses | Read and write | 如需发布状态检查时使用 |
| Metadata | Read-only | GitHub 默认需要 |

## 环境变量

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `LLM_PROVIDER` | LLM 提供方，支持 `deepseek` / `qwen` | `deepseek` |
| `DEEPSEEK_API_KEY` | DeepSeek 或 OpenAI 兼容端点 API Key | 空 |
| `DASHSCOPE_API_KEY` | DashScope / Qwen API Key | 空 |
| `CODEMATE_LLM_DEEPSEEK_BASE_URL` | OpenAI 兼容端点 Base URL | `https://api.deepseek.com` |
| `CODEMATE_LLM_DEEPSEEK_MODEL` | deepseek 分支使用的模型名 | `deepseek-chat` |
| `GITHUB_TOKEN` | GitHub PAT，用于读取 PR 和发布评论 | 空 |
| `GITHUB_WEBHOOK_SECRET` | GitHub Webhook HMAC-SHA256 共享密钥 | 空 |
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://postgres:5432/codemate` |
| `DB_USER` | 数据库用户名 | `codemate` |
| `DB_PASSWORD` | 数据库密码 | `codemate` |
| `REDIS_HOST` | Redis 主机 | `redis` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `CODEMATE_QUEUE_ENABLED` | 是否启用 Redis Stream 异步队列 | `true` |
| `CODEMATE_RAG_ENABLED` | 是否启用 RAG 项目上下文检索 | `false` |
| `CODEMATE_RAG_EMBEDDING_API_KEY` | RAG embedding 服务 API Key | 空 |

## 仓库级配置

在被评审的目标仓库根目录放置 `.codemate.yml`，即可控制评审行为：

```yaml
version: 1

agents:
  bug: true
  security: true
  performance: true
  style: true
  design: false

min_severity: medium
max_comments_per_pr: 20
min_confidence: 70

exclude:
  - "**/generated/**"
  - "**/*.proto"
  - "src/test/**"

custom_rules:
  - "禁止在 Service 层使用 System.out"
  - "所有 public API 必须有 JavaDoc"

llm:
  provider: deepseek
  model: deepseek-chat
  max_tokens_per_review: 50000
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `agents.*` | 单独开关 Bug / Security / Performance / Style / Design Agent |
| `min_severity` | 最低评论严重级别：`low` / `medium` / `high` / `critical` |
| `max_comments_per_pr` | 单个 PR 最多发布的评论数量 |
| `min_confidence` | 最低置信度，低于该值的评论会被过滤 |
| `exclude` | glob 排除规则，例如生成代码、proto、测试目录 |
| `custom_rules` | 注入到 Prompt 的自定义自然语言审查规则 |
| `llm.*` | 覆盖该仓库使用的模型和 token 预算 |

## 本地开发

编译全部模块：

```bash
mvn clean package -DskipTests
```

运行单元测试：

```bash
mvn test
```

运行包含 Docker 依赖的集成测试：

```bash
mvn test -DexcludedGroups=
```

只启动 API 模块：

```bash
mvn -pl api spring-boot:run
```

如果你希望在 Windows 主机用 IDE 调试应用、在 Linux VM 中运行 PostgreSQL 和 Redis，可以参考 [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)。

## 常见问题

### Webhook 返回 401

通常是 `GITHUB_WEBHOOK_SECRET` 不一致，或请求头 `X-Hub-Signature-256` 不正确。确认 GitHub Webhook、应用环境变量和本地模拟请求使用的是同一个 secret。

### Webhook 返回 202 但 PR 没有评论

检查应用日志中是否有入队和消费记录；同时确认 `CODEMATE_QUEUE_ENABLED=true`，Redis 可连接，GitHub Token 有 `Pull requests: Read and write` 权限。

### LLM 返回内容解析失败

Agent 期望模型返回结构化 JSON。建议使用支持 JSON 输出较稳定的模型，或调低自定义 Prompt 中可能诱导模型输出非 JSON 文本的内容。

### 启用 RAG 后启动或评审失败

先确认 PostgreSQL 镜像是 `pgvector/pgvector:pg16`，并正确配置 embedding API Key。初次部署建议先保持 `CODEMATE_RAG_ENABLED=false`，等基础 PR 评审链路跑通后再开启。

## 开源前检查

开源前建议确认：

- 删除 `application.yml`、本地部署文档、提交历史中的真实 API Key、GitHub Token、内网 IP、个人仓库名等敏感信息。
- 轮换已经提交过的 GitHub PAT、LLM API Key 和 Webhook Secret。
- 补充 `LICENSE` 文件，并将 README 中的 License 状态改成对应协议。
- 确认 `.gitignore` 覆盖 `.env`、`application-dev.yml`、`application-local.yml` 等本地配置文件。
- 使用一个干净的新仓库或公开测试仓库验证 Webhook、Token 权限和评论发布流程。

## Roadmap

- GitHub App 安装 token 支持，替代 PAT
- Web Dashboard 展示评审历史、Agent 输出和评论状态
- CLI 模式，支持本地 diff / patch 评审
- GitLab / Gitee Webhook 适配
- 更完善的 RAG 索引更新策略
- 更多语言的 AST Parser 支持

## License

TBD. 开源前请补充明确的 `LICENSE` 文件。
