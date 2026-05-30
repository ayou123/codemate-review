package com.codemate.review.github.publisher;

import com.codemate.review.core.enums.Severity;
import com.codemate.review.core.model.ReviewComment;
import com.codemate.review.core.model.ReviewResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CommentPublisher {
    private static final Logger log = LoggerFactory.getLogger(CommentPublisher.class);

    private final String apiBase;
    private final String token;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    public CommentPublisher(String apiBase, String token) {
        this.apiBase = apiBase.endsWith("/") ? apiBase.substring(0, apiBase.length() - 1) : apiBase;
        this.token = token;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public void publish(String owner, String repo, int pr, ReviewResult result) throws Exception {
        List<Map<String, Object>> inline = new ArrayList<>();
        for (ReviewComment c : result.comments()) {
            if (!c.severity().atLeast(Severity.HIGH)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("path", c.filePath());
            m.put("line", c.line());
            m.put("side", "RIGHT");
            m.put("body", formatInline(c));
            inline.add(m);
        }
        Map<String, Object> reviewBody = new LinkedHashMap<>();
        reviewBody.put("event", "COMMENT");
        reviewBody.put("body", renderSummaryBody(result));
        reviewBody.put("comments", inline);
        post("/repos/" + owner + "/" + repo + "/pulls/" + pr + "/reviews", reviewBody);

        if (result.headSha() != null && !result.headSha().isBlank()) {
            String state = result.criticalCount() > 0 ? "failure" : "success";
            Map<String, Object> status = new LinkedHashMap<>();
            status.put("state", state);
            status.put("context", "codemate-review");
            status.put("description", "critical=" + result.criticalCount()
                + " high=" + result.highCount()
                + " score=" + result.overallScore());
            post("/repos/" + owner + "/" + repo + "/statuses/" + result.headSha(), status);
        }
    }

    private String formatInline(ReviewComment c) {
        StringBuilder sb = new StringBuilder();
        sb.append("🤖 **CodeMate Review** · [").append(c.severity()).append("] · ").append(c.agentName()).append("\n\n");
        sb.append("**").append(c.title()).append("**\n\n");
        if (c.description() != null && !c.description().isBlank()) sb.append(c.description()).append("\n\n");
        if (c.suggestion() != null && !c.suggestion().isBlank()) sb.append("💡 **建议**：\n").append(c.suggestion()).append("\n\n");
        if (c.suggestedCode() != null && !c.suggestedCode().isBlank()) {
            sb.append("```suggestion\n").append(c.suggestedCode()).append("\n```\n\n");
        }
        sb.append("<sub>Confidence: ").append(c.confidence()).append("%");
        if (c.references() != null && !c.references().isEmpty()) {
            sb.append(" · ").append(String.join(", ", c.references()));
        }
        sb.append("</sub>");
        return sb.toString();
    }

    private String renderSummaryBody(ReviewResult r) {
        StringBuilder b = new StringBuilder();
        b.append("## 🤖 CodeMate Review 总报告\n\n");
        if (r.comments().isEmpty()) {
            b.append("✅ 未发现问题。整体评分: ").append(r.overallScore()).append("/100。\n");
            return b.toString();
        }
        b.append("**整体评分:** ").append(r.overallScore()).append("/100\n\n");
        b.append("| Severity | Count |\n|---|---|\n");
        b.append("| CRITICAL | ").append(r.criticalCount()).append(" |\n");
        b.append("| HIGH | ").append(r.highCount()).append(" |\n");
        b.append("| MEDIUM | ").append(r.mediumCount()).append(" |\n");
        b.append("| LOW | ").append(r.lowCount()).append(" |\n\n");
        var mediums = r.comments().stream().filter(c -> c.severity() == Severity.MEDIUM).toList();
        if (!mediums.isEmpty()) {
            b.append("### Medium 级别问题\n");
            for (ReviewComment c : mediums) {
                b.append("- ").append(c.filePath()).append(":").append(c.line())
                 .append(" — ").append(c.title()).append("\n");
            }
        }
        return b.toString();
    }

    private void post(String path, Map<String, Object> body) throws Exception {
        var req = HttpRequest.newBuilder(URI.create(apiBase + path))
            .header("Authorization", "Bearer " + token)
            .header("User-Agent", "codemate-review")
            .header("Content-Type", "application/json")
            .header("Accept", "application/vnd.github+json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
            .build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            log.warn("POST {} -> {} body={}", path, resp.statusCode(), resp.body());
            throw new RuntimeException("POST " + path + " failed " + resp.statusCode());
        }
    }
}
