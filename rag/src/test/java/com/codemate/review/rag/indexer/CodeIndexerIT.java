package com.codemate.review.rag.indexer;

import com.codemate.review.parser.JavaCodeParser;
import com.codemate.review.rag.embedding.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = CodeIndexerIT.TestContext.class,
    properties = {"codemate.rag.enabled=true", "spring.flyway.enabled=false"})
@Testcontainers
@Tag("docker")
class CodeIndexerIT {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = "com.codemate.review.rag")
    static class TestContext {
        @Bean
        JavaCodeParser javaCodeParser() { return new JavaCodeParser(); }
    }

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void p(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @MockBean
    EmbeddingService embeddings;
    @Autowired
    CodeIndexer indexer;
    @Autowired
    CodeEmbeddingDao dao;
    @Autowired
    DataSource ds;

    @BeforeEach
    void setupSchema() throws Exception {
        try (var conn = ds.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("CREATE EXTENSION IF NOT EXISTS vector");
            stmt.execute("DROP TABLE IF EXISTS code_embeddings");
            stmt.execute("""
                CREATE TABLE code_embeddings (
                    id BIGSERIAL PRIMARY KEY,
                    repo_id BIGINT,
                    file_path VARCHAR(500),
                    method_name VARCHAR(200),
                    code_chunk TEXT,
                    embedding VECTOR(3)
                )""");
        }
    }

    @Test
    void indexesAllJavaMethodsOfRepository() {
        when(embeddings.embedBatch(any())).thenAnswer(inv -> {
            List<String> in = inv.getArgument(0);
            return in.stream().map(s -> new float[]{0.1f, 0.2f, 0.3f}).toList();
        });
        indexer.indexRepository(1L, Map.of(
            "UserService.java", "public class UserService { public User getUser(Long id){ return null; } }",
            "OrderService.java", "public class OrderService { public void place(){} }"));

        var rows = dao.findByRepoId(1L);
        assertThat(rows).extracting(c -> c.methodName()).contains("getUser", "place");
    }
}
