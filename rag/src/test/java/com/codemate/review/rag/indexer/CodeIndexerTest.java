package com.codemate.review.rag.indexer;

import com.codemate.review.parser.JavaCodeParser;
import com.codemate.review.parser.MethodInfo;
import com.codemate.review.rag.embedding.CodeChunk;
import com.codemate.review.rag.embedding.EmbeddingService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CodeIndexerTest {
    @Test
    void indexerStagesEmbedsAndInserts() {
        var parser = mock(JavaCodeParser.class);
        var embeddings = mock(EmbeddingService.class);
        var dao = mock(CodeEmbeddingDao.class);
        when(parser.parseMethods(any(), eq("U.java")))
            .thenReturn(List.of(new MethodInfo("U.java", "U", "getUser", 1, 2, "public void getUser(){}", List.of())));
        when(embeddings.embedBatch(any())).thenReturn(List.of(new float[]{0.1f, 0.2f, 0.3f}));

        new CodeIndexer(parser, embeddings, dao)
            .indexRepository(1L, Map.of("U.java", "public class U { public void getUser(){} }"));

        verify(dao).deleteByRepoId(1L);
        verify(dao).insert(any(CodeChunk.class));
    }

    @Test
    void emptyParseResultDoesNothing() {
        var parser = mock(JavaCodeParser.class);
        var embeddings = mock(EmbeddingService.class);
        var dao = mock(CodeEmbeddingDao.class);
        when(parser.parseMethods(any(), any())).thenReturn(List.of());

        new CodeIndexer(parser, embeddings, dao).indexRepository(1L, Map.of("E.java", "// empty"));
        verifyNoInteractions(embeddings, dao);
    }
}
