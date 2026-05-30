package com.codemate.review.parser;

import java.util.List;

public record MethodInfo(
    String filePath,
    String className,
    String methodName,
    int startLine,
    int endLine,
    String fullCode,
    List<String> calleeMethodNames
) {
    public MethodInfo {
        calleeMethodNames = calleeMethodNames == null ? List.of() : List.copyOf(calleeMethodNames);
    }
}
