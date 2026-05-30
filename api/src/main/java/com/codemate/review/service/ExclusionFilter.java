package com.codemate.review.service;

import com.codemate.review.parser.DiffHunk;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Collectors;

public class ExclusionFilter {
    public List<DiffHunk> filter(List<DiffHunk> hunks, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) return hunks;
        List<PathMatcher> matchers = patterns.stream()
            .map(p -> FileSystems.getDefault().getPathMatcher("glob:" + p))
            .collect(Collectors.toList());
        return hunks.stream()
            .filter(h -> matchers.stream().noneMatch(m -> m.matches(Path.of(h.filePath()))))
            .toList();
    }
}
