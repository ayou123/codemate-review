package com.codemate.review.service;

import com.codemate.review.parser.DiffHunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExclusionFilterTest {

    private DiffHunk hunk(String path) {
        return new DiffHunk(path, 1, 1, List.of(1), List.of(), "+ x\n");
    }

    @Test
    void filtersOutMatchingPaths() {
        var f = new ExclusionFilter();
        var hunks = List.of(hunk("src/main/java/A.java"), hunk("src/test/java/A.java"),
            hunk("build/generated/X.java"));
        var out = f.filter(hunks, List.of("src/test/**", "**/generated/**"));
        assertThat(out).extracting(DiffHunk::filePath)
            .containsExactly("src/main/java/A.java");
    }

    @Test
    void emptyPatternsKeepAll() {
        var f = new ExclusionFilter();
        var hunks = List.of(hunk("A.java"), hunk("B.java"));
        assertThat(f.filter(hunks, List.of())).hasSize(2);
        assertThat(f.filter(hunks, null)).hasSize(2);
    }
}
