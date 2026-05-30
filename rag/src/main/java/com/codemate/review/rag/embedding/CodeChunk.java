package com.codemate.review.rag.embedding;

public record CodeChunk(long repoId, String filePath, String methodName, String codeChunk, float[] embedding) {}
