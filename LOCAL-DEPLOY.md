#本地或者VM跑docker
# 在 VM 内执行
ip addr show | grep "inet " | grep -v 127.0.0.1
# 假设输出 192.168.31.100 后面的vm ip就一直用这个代替

docker-compose.yml内容
```yml
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

```

docker compose up -d
docker compose ps 


```powershell
java --version    # 必须 21+
mvn --version     # 必须 3.8+
```

你需要设置三个环境变量：

| 环境变量 | 说明 | 示例 |
|---|---|---|
| `LLM_PROVIDER` | 固定填 `deepseek`（其实是"OpenAI 兼容"分支） | `deepseek` |
| `DEEPSEEK_API_KEY` | 你第三方端点的 API Key | `sk-xxx` |
| `CODEMATE_LLM_DEEPSEEK_BASE_URL` | 第三方端点的 base URL（**不要带 `/v1`**，代码会自己加） | `https://api.your-llm.com` |
| `CODEMATE_LLM_DEEPSEEK_MODEL` | 模型名 | `gpt-4o-mini` / `claude-3-5-sonnet` / `qwen-plus` 等 |

## GITHUB_WEBHOOK_SECRET 和 GITHUB_TOKEN 怎么配？
这两个的获取方式完全不同。

1️⃣ GITHUB_TOKEN —— GitHub Personal Access Token (PAT)
用途： app 用它去调 GitHub API（拉取 PR diff、发表 review 评论、读取文件等）。

获取步骤：

浏览器打开 https://github.com/settings/tokens?type=beta （Fine-grained PAT，推荐）
Generate new token
Token name: 随便填，例如 codemate-review-local
Expiration: 30 days 或更短
Repository access: 选 Only select repositories → 选你打算用来测试的那一个或几个仓库
Repository permissions: 至少要给：
Contents: Read-only （读 PR 文件内容）
Pull requests: Read and write （读 PR + 发评论）
Metadata: Read-only （自动包含，必须）
Commit statuses: Read and write （发 build status）
拉到最下面点 Generate token
立刻复制 这个 github_pat_xxxx... 串 —— 关掉页面就再也看不到了
老式 Classic PAT 也行（https://github.com/settings/tokens/new），勾 repo scope。但 fine-grained 更安全。

2️⃣ GITHUB_WEBHOOK_SECRET —— 你自己生成的密码
用途： GitHub 发 webhook 时用这个 secret 做 HMAC-SHA256 签名；app 用同一个 secret 验签。两边必须一致。

获取步骤：自己生成一个随机串就行：


# PowerShell 生成 32 字节随机 hex
-join ((0..31) | %{ '{0:x2}' -f (Get-Random -Max 256) })
# 例如输出: 7f4a2e9c8b1d3f5a6e0c2b4d8f1a3e5c
或者直接想一个，例如 my-codemate-secret-2026。这就是个对称的共享密码，你定就行。



## 接入github
适合最终演示。

1. 装 ngrok

下载 https://ngrok.com/download 解压到 PATH，注册账号拿 authtoken：


ngrok config add-authtoken <你的 ngrok token>
ngrok http 8080
输出里找：


Forwarding   https://xxxx-xxxx.ngrok-free.app -> http://localhost:8080
2. GitHub 仓库配 webhook

仓库 → Settings → Webhooks → Add webhook：

字段	值
Payload URL	https://xxxx-xxxx.ngrok-free.app/webhook/github
Content type	application/json
Secret	my-codemate-secret-2026 （跟 application-dev.yml 一致）
Which events?	Let me select → 勾 Pull requests
Active	✅
点 Add webhook。GitHub 会立刻发一个 ping 事件，看 ngrok 日志应该是 POST /webhook/github 204 No Content（因为不是 pull_request 事件，app 返回 204 是正常）。

3. 触发

去那个仓库新建一个分支 → 改一个 .java 文件 → 提 PR
或者重新打开一个已有的 PR
几秒后 PR 页面应该出现 CodeMate Review 的评论。

#赋予token权限
浏览器打开 https://github.com/settings/personal-access-tokens
找到你之前用的那个 token → 点 Edit （如果找不到就 Generate new token）
Repository access:
选 Only select repositories → 勾选 ayou123/miniprogram-1
Repository permissions 必须全勾这几个：
Contents → Read-only
Pull requests → ⚠ Read and write（关键！这一项之前你没勾）
Commit statuses → 设为 Read and write
Metadata → Read-only（自动包含）



验证 webhook 真的能跑通
ping 通了之后，真正的测试是触发一个 PR 事件：

去那个仓库新建分支：git checkout -b test-codemate
改一个 .java 文件（随便加一行代码）
push + 提一个 PR
立刻看：
ngrok 控制台 http://localhost:4040 — 应该看到 POST /webhook/github 进来
IDE 控制台 — 应该看到 enqueued review your-user/your-repo pr#1 之后是一堆 Agent 调用
几十秒后 PR 页面应该出现 CodeMate Review 的评论

准备一个真实的 PR
在你那个配了 webhook 的仓库里：


cd <你的某个本地 Java 仓库克隆>
git checkout -b test-codemate
随便改一个 .java 文件 —— 制造点能让 Agent 评论的内容效果更好：


// 在某个文件里加这么一个有"问题"的方法
public class Demo {
public String getUserName(User user) {
return user.getName();   // 故意没做 null 检查，BugAgent 应该会发现
}
}