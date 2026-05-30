package com.codemate.review.service;

import com.codemate.review.github.client.GitHubClient;
import com.codemate.review.persistence.entity.RepositoryEntity;
import com.codemate.review.persistence.repository.RepositoryRepository;
import com.codemate.review.rag.indexer.CodeIndexer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IndexingServiceTest {
    @SuppressWarnings("unchecked")
    private ObjectProvider<CodeIndexer> providerOf(CodeIndexer ci) {
        ObjectProvider<CodeIndexer> mock = mock(ObjectProvider.class);
        when(mock.getIfAvailable()).thenReturn(ci);
        return mock;
    }

    @Test
    void triggersIndexingOnFirstReview() throws Exception {
        CodeIndexer indexer = mock(CodeIndexer.class);
        GitHubClient gh = mock(GitHubClient.class);
        RepositoryRepository repoRepo = mock(RepositoryRepository.class);
        when(gh.listRootJavaFiles(any(), any(), any())).thenReturn(List.of("A.java"));
        when(gh.fetchFile(any(), any(), any(), eq("A.java"))).thenReturn(Optional.of("class A{}"));

        var repo = new RepositoryEntity();
        repo.setId(1L);
        repo.setFullName("o/r");

        new IndexingService(providerOf(indexer), gh, repoRepo).ensureIndexed(repo, "sha");

        verify(indexer, timeout(2000)).indexRepository(eq(1L), any());
        verify(repoRepo, timeout(2000)).save(argThat((RepositoryEntity r) -> r.getIndexedAt() != null));
    }

    @Test
    void skipsWhenAlreadyIndexed() {
        CodeIndexer indexer = mock(CodeIndexer.class);
        var repo = new RepositoryEntity();
        repo.setIndexedAt(LocalDateTime.now());
        new IndexingService(providerOf(indexer), mock(GitHubClient.class), mock(RepositoryRepository.class))
            .ensureIndexed(repo, "sha");
        verifyNoInteractions(indexer);
    }

    @Test
    void noOpWhenIndexerUnavailable() {
        var repo = new RepositoryEntity();
        new IndexingService(providerOf(null), mock(GitHubClient.class), mock(RepositoryRepository.class))
            .ensureIndexed(repo, "sha");
        // Nothing throws
    }
}
