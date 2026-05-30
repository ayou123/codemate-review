# 本地部署调试文档（Windows + VM 场景）

面向以下场景：
- 主机：Windows
- 数据库 / Redis：跑在虚拟机里（Linux）
- LLM：第三方 **OpenAI 兼容** 端点（不是 DeepSeek 官方）
- 调试：在 Windows 上用 IDE 启动 `CodemateApplication`，连 VM 里的 postgres/redis

---

## 一、拓扑图

```
┌────────────────────────────────┐         ┌──────────────────────────┐
│   Windows 主机                  │         │   Linux 虚拟机            │
│                                │         │                          │
│   IDE (IntelliJ / VSCode)      │         │   docker compose:        │
│      │                         │   TCP   │   ├── postgres:5432      │
│      ├─ CodemateApplication ──┼─────────┤   └── redis:6379         │
│      │   (Spring Boot, :8080)  │         │                          │
│      │                         │         │   VM IP: 192.168.x.x     │
│      └─ curl / Postman         │         │                          │
└──────┬─────────────────────────┘         └──────────────────────────┘
       │
       │ HTTPS
       ▼
┌──────────────────────────────────────────┐
│  第三方 OpenAI 兼容 LLM 端点              │
│  (例: https://api.your-llm.com)          │
└──────────────────────────────────────────┘
```

**为什么这么部署：** Windows 跑 Docker 通常用 WSL2 或 Docker Desktop，资源占用大；放 VM 里干净隔离。app 本身留在主机 IDE 里，可以打断点、热重启，调试体验最好。

---

## 二、虚拟机准备 Postgres + Redis

### 2.1 VM 网络模式

确保虚拟机用 **桥接（Bridged）** 或 **NAT + 端口转发** 模式，让 Windows 主机能通过 IP 访问 VM 的 5432/6379 端口。

- **VirtualBox NAT 模式**：需手动加端口转发：5432→5432，6379→6379
- **VMware NAT 模式**：通常自动可达
- **桥接模式**：最简单，VM 直接拿一个局域网 IP

记下 VM IP：

```bash
# 在 VM 内执行
ip addr show | grep "inet " | grep -v 127.0.0.1
# 假设输出 192.168.31.100
```

后面所有 `<VM_IP>` 都用这个地址替换。

### 2.2 VM 装 Docker + Docker Compose

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# 重新登录，让用户组生效
```

### 2.3 只拉数据库镜像，跑两个容器

不需要把整个 `docker-compose.yml` 跑起来（app 在 Windows 跑）。在 VM 里新建一个最小的 compose 文件：

```bash
mkdir -p ~/codemate-deps && cd ~/codemate-deps
cat > docker-compose.yml <<'EOF'
services:
  postgres:
    image: pgvector/pgvector:pg16
    container_name: codemate-postgres
    environment:
      POSTGRES_DB: codemate
      POSTGRES_USER: codemate
      POSTGRES_PASSWORD: codemate
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U codemate"]
      interval: 5s
      retries: 10

  redis:
    image: redis:7-alpine
    container_name: codemate-redis
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      retries: 10

volumes:
  pgdata: {}
EOF

docker compose up -d
docker compose ps      # 两个容器都应该 healthy
```

### 2.4 从 Windows 主机验证连通

```powershell
# Windows PowerShell
Test-NetConnection -ComputerName <VM_IP> -Port 5432
Test-NetConnection -ComputerName <VM_IP> -Port 6379
# 两个都应该 TcpTestSucceeded : True
```

如果不通，检查：
- VM 防火墙：`sudo ufw allow 5432 && sudo ufw allow 6379`（Ubuntu）
- VirtualBox/VMware 端口转发

---

## 三、Windows 主机准备 Java / Maven

```powershell
java --version    # 必须 21+
mvn --version     # 必须 3.8+
```

已有就跳过。没有就装 [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21) + [Maven 3.9](https://maven.apache.org/download.cgi)。

---

## 四、配置 LLM 走第三方 OpenAI 兼容端点 ⭐

这是你**唯一需要理解的代码逻辑**。

### 4.1 现有代码怎么处理 LLM provider

[LlmConfig.java](api/src/main/java/com/codemate/review/config/LlmConfig.java) 有两个分支：

```java
@Bean
@ConditionalOnProperty(name = "codemate.llm.provider", havingValue = "deepseek", matchIfMissing = true)
LlmClient deepseekClient(...) {
    return LangChain4jLlmClient.deepseek(apiKey, baseUrl, model);
}

@Bean
@ConditionalOnProperty(name = "codemate.llm.provider", havingValue = "qwen")
LlmClient qwenClient(...) { ... }
```

而 [LangChain4jLlmClient.deepseek()](agent/src/main/java/com/codemate/review/agent/LangChain4jLlmClient.java) 内部其实是 **完全的 OpenAI 兼容客户端**：

```java
public static LangChain4jLlmClient deepseek(String apiKey, String baseUrl, String modelName) {
    // 自动给 baseUrl 加 /v1 后缀，最终调用 ${baseUrl}/chat/completions
    var llm = OpenAiChatModel.builder()
        .apiKey(apiKey).baseUrl(fullBaseUrl).modelName(modelName)
        .responseFormat("json_object").build();
    return new LangChain4jLlmClient(llm, "deepseek");
}
```

**结论：** "deepseek" 这个 provider 名字只是历史命名，实际上任何 **OpenAI Chat Completion API 兼容**的端点（你的代理、OpenAI 官方、OneAPI、新 API、Moonshot、智谱、kimi 等）都能用。

### 4.2 三个关键配置项

你需要设置三个环境变量：

| 环境变量 | 说明 | 示例 |
|---|---|---|
| `LLM_PROVIDER` | 固定填 `deepseek`（其实是"OpenAI 兼容"分支） | `deepseek` |
| `DEEPSEEK_API_KEY` | 你第三方端点的 API Key | `sk-xxx` |
| `CODEMATE_LLM_DEEPSEEK_BASE_URL` | 第三方端点的 base URL（**不要带 `/v1`**，代码会自己加） | `https://api.your-llm.com` |
| `CODEMATE_LLM_DEEPSEEK_MODEL` | 模型名 | `gpt-4o-mini` / `claude-3-5-sonnet` / `qwen-plus` 等 |

> ⚠ Spring 把 `codemate.llm.deepseek.base-url` 映射成环境变量是 `CODEMATE_LLM_DEEPSEEK_BASEURL`（下划线，无 dash）。Spring Boot Relaxed Binding 也支持 `CODEMATE_LLM_DEEPSEEK_BASE_URL` 写法 — 推荐用下面这种 `application-dev.yml` 方式，避免环境变量大小写踩坑。

### 4.3（可选）改名让代码更清晰

如果你介意 "deepseek" 这个名字 — 改起来很简单，**两处**：

1. [LlmConfig.java](api/src/main/java/com/codemate/review/config/LlmConfig.java)：把 `havingValue = "deepseek"` 改成 `havingValue = "openai"`，把 yaml key `codemate.llm.deepseek.*` 改成 `codemate.llm.openai.*`，再把工厂方法名从 `LangChain4jLlmClient.deepseek` 改成 `LangChain4jLlmClient.openai`
2. [LangChain4jLlmClient.java](agent/src/main/java/com/codemate/review/agent/LangChain4jLlmClient.java)：把工厂方法 `public static LangChain4jLlmClient deepseek(...)` 改名为 `openai(...)`，把 `provider` 字段值改成 `"openai"`

我不强制你改 — 用环境变量绕过更省事。

### 4.4 用 `application-dev.yml` 覆盖配置（推荐做法）

不要污染默认的 `application.yml`，新建一个 dev profile：

新建文件 `api/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://<VM_IP>:5432/codemate
    username: codemate
    password: codemate
  jpa:
    hibernate:
      ddl-auto: validate    # 由 Flyway 管理 schema
  data:
    redis:
      host: <VM_IP>
      port: 6379
  flyway:
    enabled: true

codemate:
  github:
    webhook-secret: topsecret-change-me
    app-token: ghp_your_github_pat_here
    api-base: https://api.github.com
  llm:
    provider: deepseek          # = OpenAI 兼容分支
    deepseek:
      api-key: sk-your-third-party-key
      base-url: https://api.your-llm.com    # 注意不要加 /v1
      model: gpt-4o-mini                    # 或其他兼容模型
  review:
    max-comments-per-pr: 20
    min-confidence: 70
    max-tokens-per-review: 50000
  queue:
    enabled: true               # 开启 Redis Stream 消费
  rag:
    enabled: false              # 初次启动建议先关，跑通基础链路再开
```

把 `<VM_IP>`、`api-key`、`base-url`、`app-token`、`webhook-secret` 都改成你的真实值。

> 文件已被 `.gitignore` 排除吗？— 当前 `.gitignore` 不会排除 `application-dev.yml`。**别 commit 这个文件**（它带 secret），手动加一行到 `.gitignore`：
> ```
> application-dev.yml
> application-local.yml
> ```

---

## 五、Build + Run

### 5.1 一次性编译（让所有模块 install 到本地 Maven 仓库）

```powershell
cd d:\code\product\autoCodeReview-agent
mvn clean install -DskipTests
```

约 1 分钟。

### 5.2 启动 app

**方式 A：IDE 启动（推荐，能调试）**

在 IntelliJ：
- 右键 [CodemateApplication.java](api/src/main/java/com/codemate/review/CodemateApplication.java) → Run
- 在 Run Configuration 加：
  - **Active profiles:** `dev`
  - **VM options:** `-Dspring.profiles.active=dev`

**方式 B：命令行启动**

```powershell
mvn -pl api spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### 5.3 启动成功的标志

控制台看到：

```
Started CodemateApplication in N.NN seconds
started Redis Stream listener container
```

然后另开终端验证：

```powershell
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

> `actuator/health` 默认只显示 UP/DOWN。要看 DB/Redis 详情，临时加到 `application-dev.yml`：
> ```yaml
> management:
>   endpoint:
>     health:
>       show-details: always
> ```

---

## 六、本地模拟 Webhook 测试（不依赖真 GitHub）

最快验证整条链路是否通。

### 6.1 准备一段 PR payload

新建 `payload.json`（放任意位置）：

```json
{
  "action": "opened",
  "pull_request": {
    "number": 1,
    "head": { "sha": "0000000000000000000000000000000000000123" },
    "base": { "sha": "0000000000000000000000000000000000000456" }
  },
  "repository": {
    "full_name": "your-github-username/your-real-repo"
  },
  "installation": {
    "id": 42
  }
}
```

注意：`full_name` **必须**是一个真存在的仓库 — 否则 `GitHubClient.fetchPR` 会 404，整链失败。建议拿你自己的某个公开仓库的最新 PR（如果有），并把 `head.sha` 改成那个 PR 的真实 head SHA。

### 6.2 用 PowerShell 计算签名 + 发请求

```powershell
$body = Get-Content -Raw payload.json
$secret = "topsecret-change-me"   # 与 application-dev.yml 里的 webhook-secret 一致

# 计算 HMAC-SHA256
$hmac = New-Object System.Security.Cryptography.HMACSHA256
$hmac.Key = [Text.Encoding]::UTF8.GetBytes($secret)
$hash = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($body))
$sig = "sha256=" + ([BitConverter]::ToString($hash) -replace '-','').ToLower()

# 发请求
curl.exe -X POST http://localhost:8080/webhook/github `
  -H "X-GitHub-Event: pull_request" `
  -H "X-Hub-Signature-256: $sig" `
  -H "Content-Type: application/json" `
  --data $body
```

**期望：** 立即返回 `202 Accepted`，几秒后控制台日志出现：

```
enqueued review your-user/your-repo pr#1
[stub or real] runReview ...
agent bug ...
posted review to /repos/.../pulls/1/reviews
```

如果 `your-real-repo` PR 真实存在，去 GitHub 上看那个 PR — 应该收到 CodeMate 的 review 评论。

### 6.3 不想真打 GitHub 的快速烟雾测试

如果只想验证 webhook → queue → consumer 这一段，不想真去 GitHub：

把 `your-github-username/your-real-repo` 换成假仓库名。链路会跑到 `GitHubClient.fetchPR()` 报 404，但你能在日志里看到：

```
enqueued review fake/repo pr#1
review failed for fake/repo pr#1
```

证明 webhook 接收 / 签名校验 / Redis Stream 入队 / Consumer 拉取 / Service 触发 — 五个环节都通。

---

## 七、接真 GitHub Webhook（公网调试）

本地 `localhost:8080` 公网不可达，得开隧道。

### 7.1 安装 ngrok（最简单）

```powershell
# 用 winget 或去 https://ngrok.com 下
winget install ngrok
ngrok config add-authtoken <你的token>
ngrok http 8080
```

记下分配的 HTTPS URL，例如 `https://abcd-1234.ngrok-free.app`。

### 7.2 GitHub 仓库配 webhook

到目标仓库 → **Settings** → **Webhooks** → **Add webhook**：

| 字段 | 值 |
|---|---|
| Payload URL | `https://abcd-1234.ngrok-free.app/webhook/github` |
| Content type | `application/json` |
| Secret | 跟 `application-dev.yml` 里的 `webhook-secret` 一致 |
| Which events? | "Let me select" → 勾选 **Pull requests** |
| Active | ✅ |

点 Add webhook。GitHub 会自动发一个 `ping` 事件，你的 app 应回 `204 No Content`（因为不是 `pull_request` 事件）。GitHub Webhook Deliveries 页能看到 ✅。

之后你新开 PR / push 到 PR 时，会自动触发 review。

---

## 八、排查 cheatsheet

| 现象 | 排查 |
|---|---|
| `mvn package` 失败 | 看堆栈第一行，多半是依赖下载失败 — 换 maven 镜像源 |
| 启动报 `Connection refused: localhost/127.0.0.1:5432` | 没用 `dev` profile，或 `application-dev.yml` 的 DB url 写错 |
| 启动报 `Could not open JDBC Connection ... Connection refused: <VM_IP>/5432` | Windows 主机连不到 VM postgres — 看第 2.4 节排查 |
| 启动报 `relation "repositories" does not exist` | Flyway 没跑，确认 `spring.flyway.enabled=true` 且 `ddl-auto: validate` |
| 启动报 `pgvector extension does not exist` | postgres 镜像不对，必须用 `pgvector/pgvector:pg16`，不能用 `postgres:16` |
| Webhook 返回 `401` | 签名不对，secret 不一致 — 检查 `webhook-secret` 三处（GitHub、app config、curl 算签名时） |
| Webhook 返回 `202` 但啥都不发生 | `codemate.queue.enabled` 没开成 `true`，consumer 没启动 |
| 日志：`review failed: 401 Unauthorized` | GitHub `app-token` 不对或权限不够 — token 至少要 `repo` 权限 |
| 日志：`agent bug failed to parse LLM output` | LLM 返回的不是 JSON，模型不行 — 换个支持 JSON mode 的模型（`gpt-4o-mini` / `gpt-4o` / `qwen-plus` 等） |
| 日志：`embedding failed 401` | 启用了 `rag.enabled=true` 但没配 embedding API key — 暂时关 RAG，先把基础链路跑通 |
| 评论一直没出现，但日志有 `posted review to ...` | GitHub PR 已经评论过同一行 — 重新提 commit，head sha 变了才会重审 |
| 调试很慢，每次都要 push | 用第六节的本地 curl 模拟 — 无需真 PR |

### 看日志技巧

`application-dev.yml` 加：

```yaml
logging:
  level:
    com.codemate.review: DEBUG
    org.springframework.data.redis.stream: INFO
```

最关注的几行：

```
WebhookController          - rejected webhook: bad signature      ← 签名问题
ReviewJobProducer          - enqueued review {repo} pr#{n}        ← 入队成功
ReviewJobConsumer          - skipping stale job for ...           ← 取消机制工作
ReviewService              - skipping {file} (1500 lines > 1000)  ← 大文件跳过
ReviewOrchestrator         - budget exhausted ... skipping {agent}← Token 预算用完
CommentPublisher           - POST /repos/.../reviews → 200        ← 评论发布成功
```

### Redis Stream 手动查看

VM 里：

```bash
docker exec -it codemate-redis redis-cli
> XLEN codemate.reviews              # 队列里多少条
> XRANGE codemate.reviews - +        # 看全部消息
> KEYS cancel:*                       # 看取消标记
> XINFO GROUPS codemate.reviews       # consumer group 状态
```

### Postgres 手动查看

VM 里：

```bash
docker exec -it codemate-postgres psql -U codemate -d codemate
> \dt                                  # 列所有表
> SELECT id, full_name FROM repositories;
> SELECT id, pr_number, status, overall_score FROM reviews ORDER BY id DESC LIMIT 5;
> SELECT severity, count(*) FROM review_comments GROUP BY severity;
```

---

## 九、关闭 / 清理

```bash
# VM 内停 db/redis（保留数据）
docker compose down

# VM 内停并删数据
docker compose down -v
```

```powershell
# Windows 上 IDE 直接 Stop
# 命令行启动的话 Ctrl+C
```

---

## 十、改造建议（按需取用）

- **本地不想真打第三方 LLM API**（省 token）：暂时把 `codemate.llm.provider` 设成一个不存在的值（比如 `noop`），让 `AgentConfig.noopLlmClient` 接管 — Agent 总是返回空评论，链路能跑通但不产生 review
- **加自定义 review 规则**：在被审仓库的根目录加 `.codemate.yml`（参考项目根的样例），不用改代码
- **想看请求/响应**：在 `application-dev.yml` 加 `logging.level.dev.langchain4j=DEBUG`
- **想加新 Agent**：在 [agent/impl](agent/src/main/java/com/codemate/review/agent/impl/) 新增类继承 `ReviewAgent`，写 [prompts/](agent/src/main/resources/prompts/) 模板，在 [AgentConfig](api/src/main/java/com/codemate/review/config/AgentConfig.java) 注册 `@Bean`，在 [ReviewOrchestrator.PRIORITY](agent/src/main/java/com/codemate/review/agent/orchestrator/ReviewOrchestrator.java) 加上 agent name
