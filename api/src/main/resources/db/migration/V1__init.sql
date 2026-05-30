CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE repositories (
    id BIGSERIAL PRIMARY KEY,
    github_id BIGINT UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    installation_id BIGINT,
    config JSONB,
    created_at TIMESTAMP DEFAULT now(),
    indexed_at TIMESTAMP
);

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    repo_id BIGINT REFERENCES repositories(id),
    pr_number INT NOT NULL,
    commit_sha VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    overall_score INT,
    critical_count INT DEFAULT 0,
    high_count INT DEFAULT 0,
    medium_count INT DEFAULT 0,
    low_count INT DEFAULT 0,
    llm_tokens_used INT DEFAULT 0,
    llm_cost_usd DECIMAL(10,4) DEFAULT 0,
    duration_ms INT,
    created_at TIMESTAMP DEFAULT now(),
    finished_at TIMESTAMP
);

CREATE INDEX idx_reviews_repo_pr ON reviews(repo_id, pr_number);

CREATE TABLE review_comments (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT REFERENCES reviews(id) ON DELETE CASCADE,
    agent_name VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    category VARCHAR(20) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    line INT NOT NULL,
    title TEXT,
    description TEXT,
    suggestion TEXT,
    confidence INT,
    github_comment_id BIGINT
);

CREATE TABLE code_embeddings (
    id BIGSERIAL PRIMARY KEY,
    repo_id BIGINT REFERENCES repositories(id) ON DELETE CASCADE,
    file_path VARCHAR(500) NOT NULL,
    method_name VARCHAR(200),
    code_chunk TEXT,
    embedding VECTOR(1536),
    metadata JSONB,
    indexed_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_code_embeddings_ann ON code_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
