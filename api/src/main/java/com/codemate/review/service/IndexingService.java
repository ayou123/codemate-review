package com.codemate.review.service;

import com.codemate.review.github.client.GitHubClient;
import com.codemate.review.persistence.entity.RepositoryEntity;
import com.codemate.review.persistence.repository.RepositoryRepository;
import com.codemate.review.rag.indexer.CodeIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@ConditionalOnProperty(name = "codemate.queue.enabled", havingValue = "true", matchIfMissing = true)
public class IndexingService {
    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    private final ObjectProvider<CodeIndexer> indexerProvider;
    private final GitHubClient gh;
    private final RepositoryRepository repoRepo;
    private final ExecutorService bg = Executors.newVirtualThreadPerTaskExecutor();

    public IndexingService(ObjectProvider<CodeIndexer> indexerProvider, GitHubClient gh, RepositoryRepository repoRepo) {
        this.indexerProvider = indexerProvider;
        this.gh = gh;
        this.repoRepo = repoRepo;
    }

    public void ensureIndexed(RepositoryEntity repo, String sha) {
        CodeIndexer indexer = indexerProvider.getIfAvailable();
        if (indexer == null) {
            log.debug("indexer not available; RAG disabled");
            return;
        }
        if (repo.getIndexedAt() != null) {
            log.debug("already indexed: {}", repo.getFullName());
            return;
        }
        bg.submit(() -> {
            try {
                String[] or = repo.getFullName().split("/", 2);
                var paths = gh.listRootJavaFiles(or[0], or[1], sha);
                var files = new LinkedHashMap<String, String>();
                for (String p : paths) {
                    gh.fetchFile(or[0], or[1], sha, p).ifPresent(c -> files.put(p, c));
                }
                indexer.indexRepository(repo.getId(), files);
                repo.setIndexedAt(LocalDateTime.now());
                repoRepo.save(repo);
                log.info("indexed {} ({} files)", repo.getFullName(), files.size());
            } catch (Exception e) {
                log.error("indexing failed for {}", repo.getFullName(), e);
            }
        });
    }
}
