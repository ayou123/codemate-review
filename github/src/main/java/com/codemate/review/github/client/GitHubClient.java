package com.codemate.review.github.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class GitHubClient {
    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);

    private final String apiBase;
    private final String token;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    public GitHubClient(String apiBase, String token) {
        this.apiBase = apiBase.endsWith("/") ? apiBase.substring(0, apiBase.length() - 1) : apiBase;
        this.token = token;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public PRInfo fetchPR(String owner, String repo, int pr) throws Exception {
        var resp = get("/repos/" + owner + "/" + repo + "/pulls/" + pr, null);
        if (resp.statusCode() != 200) throw new RuntimeException("fetchPR " + resp.statusCode() + " " + resp.body());
        JsonNode n = json.readTree(resp.body());
        return new PRInfo(
            n.path("number").asInt(pr),
            n.path("title").asText(""),
            n.path("body").asText(""),
            n.path("base").path("sha").asText(""),
            n.path("head").path("sha").asText("")
        );
    }

    public String fetchDiff(String owner, String repo, int pr) throws Exception {
        var resp = get("/repos/" + owner + "/" + repo + "/pulls/" + pr, "application/vnd.github.v3.diff");
        if (resp.statusCode() != 200) throw new RuntimeException("fetchDiff " + resp.statusCode());
        return resp.body();
    }

    public Map<String,String> fetchFiles(String owner, String repo, String sha, List<String> paths) {
        Map<String,String> out = new LinkedHashMap<>();
        for (String p : paths) fetchFile(owner, repo, sha, p).ifPresent(c -> out.put(p, c));
        return out;
    }

    public Optional<String> fetchFile(String owner, String repo, String sha, String path) {
        try {
            var resp = get("/repos/" + owner + "/" + repo + "/contents/" + path + "?ref=" + sha, null);
            if (resp.statusCode() == 404) return Optional.empty();
            if (resp.statusCode() != 200) {
                log.warn("fetchFile {} -> {}", path, resp.statusCode());
                return Optional.empty();
            }
            JsonNode n = json.readTree(resp.body());
            String b64 = n.path("content").asText("").replace("\n", "");
            if (b64.isEmpty()) return Optional.empty();
            return Optional.of(new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("fetchFile failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public List<String> listRootJavaFiles(String owner, String repo, String sha) {
        try {
            var resp = get("/repos/" + owner + "/" + repo + "/git/trees/" + sha + "?recursive=1", null);
            if (resp.statusCode() != 200) return List.of();
            JsonNode tree = json.readTree(resp.body()).path("tree");
            List<String> result = new ArrayList<>();
            for (JsonNode node : tree) {
                if ("blob".equals(node.path("type").asText())
                    && node.path("path").asText().endsWith(".java")) {
                    result.add(node.path("path").asText());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("listRootJavaFiles failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * MVP authentication: a single PAT (personal access token) or GitHub App installation token is used
     * as a `Bearer` token for every request. Real GitHub App installations would require JWT-signing
     * the App's private key and exchanging for a short-lived installation token via
     * POST /app/installations/{installation_id}/access_tokens. That is deferred to a future task.
     * TODO(post-MVP): implement App-JWT -> installation token exchange.
     */
    private HttpResponse<String> get(String path, String accept) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(apiBase + path))
            .header("Authorization", "Bearer " + token)
            .header("User-Agent", "codemate-review")
            .timeout(Duration.ofSeconds(30))
            .GET();
        if (accept != null) builder.header("Accept", accept);
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
