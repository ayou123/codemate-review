package com.codemate.review.rag.indexer;

import com.codemate.review.parser.JavaCodeParser;
import com.codemate.review.parser.MethodInfo;
import com.codemate.review.rag.embedding.CodeChunk;
import com.codemate.review.rag.embedding.EmbeddingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "codemate.rag.enabled", havingValue = "true")
public class CodeIndexer {
    private final JavaCodeParser parser;
    private final EmbeddingService embeddings;
    private final CodeEmbeddingDao dao;

    public CodeIndexer(JavaCodeParser parser, EmbeddingService embeddings, CodeEmbeddingDao dao) {
        this.parser = parser;
        this.embeddings = embeddings;
        this.dao = dao;
    }

    public void indexRepository(long repoId, Map<String, String> filesByPath) {
        List<CodeChunk> staged = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        for (var e : filesByPath.entrySet()) {
            for (MethodInfo m : parser.parseMethods(e.getValue(), e.getKey())) {
                staged.add(new CodeChunk(repoId, e.getKey(), m.methodName(), m.fullCode(), null));
                texts.add(m.methodName() + " " + m.fullCode());
            }
        }
        if (staged.isEmpty()) return;
        var vecs = embeddings.embedBatch(texts);
        dao.deleteByRepoId(repoId);
        for (int i = 0; i < staged.size(); i++) {
            var s = staged.get(i);
            dao.insert(new CodeChunk(s.repoId(), s.filePath(), s.methodName(), s.codeChunk(), vecs.get(i)));
        }
    }
}
