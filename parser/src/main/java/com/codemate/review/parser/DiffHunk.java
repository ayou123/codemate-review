package com.codemate.review.parser;

import java.util.List;

/**
 * Structured representation of a single unified-diff hunk.
 *
 * <p>{@code addedLines} are new-side line numbers (lines present in the
 * post-change file). {@code removedLines} are old-side line numbers (lines
 * that existed in the pre-change file but were deleted).
 */
public record DiffHunk(
        String filePath,
        int newStartLine,
        int newLineCount,
        List<Integer> addedLines,
        List<Integer> removedLines,
        String hunkContent
) {
    public DiffHunk {
        addedLines = addedLines == null ? List.of() : List.copyOf(addedLines);
        removedLines = removedLines == null ? List.of() : List.copyOf(removedLines);
    }
}
