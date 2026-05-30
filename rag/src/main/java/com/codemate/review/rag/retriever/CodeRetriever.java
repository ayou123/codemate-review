package com.codemate.review.rag.retriever;

import com.codemate.review.rag.embedding.CodeChunk;
import com.codemate.review.rag.embedding.EmbeddingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@ConditionalOnProperty(name = "codemate.rag.enabled", havingValue = "true")
public class CodeRetriever {
    private final EmbeddingService embeddings;
    private final JdbcTemplate jdbc;

    public CodeRetriever(EmbeddingService embeddings, JdbcTemplate jdbc) {
        this.embeddings = embeddings;
        this.jdbc = jdbc;
    }

    public List<CodeChunk> findSimilar(long repoId, String query, int k) {
        float[] q = embeddings.embed(query);
        return jdbc.query("""
            SELECT repo_id, file_path, method_name, code_chunk
            FROM code_embeddings
            WHERE repo_id = ?
            ORDER BY embedding <=> ?::vector
            LIMIT ?""",
            (rs, i) -> new CodeChunk(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), null),
            repoId, toVectorLiteral(q), k);
    }

    static String toVectorLiteral(float[] v) {
        return "[" + IntStream.range(0, v.length).mapToObj(i -> Float.toString(v[i]))
            .collect(Collectors.joining(",")) + "]";
    }
}
