package com.codemate.review.rag.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmbeddingService {
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public EmbeddingService(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    public List<float[]> embedBatch(List<String> texts) {
        try {
            String body = json.writeValueAsString(Map.of("model", model, "input", texts));
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/embeddings"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new RuntimeException("embedding " + resp.statusCode() + " " + resp.body());
            }
            JsonNode data = json.readTree(resp.body()).path("data");
            List<float[]> out = new ArrayList<>();
            for (JsonNode entry : data) {
                JsonNode arr = entry.path("embedding");
                float[] vec = new float[arr.size()];
                for (int i = 0; i < vec.length; i++) vec[i] = arr.get(i).floatValue();
                out.add(vec);
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("EmbeddingService.embedBatch failed", e);
        }
    }
}
