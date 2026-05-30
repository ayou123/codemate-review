package com.codemate.review.rag.retriever;

import com.codemate.review.rag.embedding.CodeChunk;
import com.codemate.review.rag.embedding.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CodeRetrieverTest {
    @Test
    @SuppressWarnings("unchecked")
    void findSimilarBuildsQueryAndReturnsRows() {
        EmbeddingService emb = mock(EmbeddingService.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(emb.embed("hello")).thenReturn(new float[]{0.1f, 0.2f, 0.3f});
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong(), anyString(), anyInt()))
            .thenReturn(List.of(new CodeChunk(1L, "X.java", "foo", "code", null)));

        var out = new CodeRetriever(emb, jdbc).findSimilar(1L, "hello", 3);
        assertThat(out).extracting(CodeChunk::methodName).containsExactly("foo");
        verify(jdbc).query(anyString(), any(RowMapper.class), eq(1L), eq("[0.1,0.2,0.3]"), eq(3));
    }
}
