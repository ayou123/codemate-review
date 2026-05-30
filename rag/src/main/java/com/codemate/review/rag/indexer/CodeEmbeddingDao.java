package com.codemate.review.rag.indexer;

import com.codemate.review.rag.embedding.CodeChunk;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@ConditionalOnProperty(name = "codemate.rag.enabled", havingValue = "true")
public class CodeEmbeddingDao {
    private final JdbcTemplate jdbc;

    public CodeEmbeddingDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(CodeChunk c) {
        jdbc.update(
            "INSERT INTO code_embeddings (repo_id, file_path, method_name, code_chunk, embedding) VALUES (?, ?, ?, ?, ?::vector)",
            c.repoId(), c.filePath(), c.methodName(), c.codeChunk(), toVectorLiteral(c.embedding()));
    }

    public void deleteByRepoId(long repoId) {
        jdbc.update("DELETE FROM code_embeddings WHERE repo_id = ?", repoId);
    }

    public List<CodeChunk> findByRepoId(long repoId) {
        return jdbc.query(
            "SELECT repo_id, file_path, method_name, code_chunk FROM code_embeddings WHERE repo_id = ?",
            (rs, i) -> new CodeChunk(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), null),
            repoId);
    }

    static String toVectorLiteral(float[] v) {
        return "[" + IntStream.range(0, v.length).mapToObj(i -> Float.toString(v[i]))
            .collect(Collectors.joining(",")) + "]";
    }
}
