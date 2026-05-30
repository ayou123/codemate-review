package com.codemate.review.parser;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PRDiffParserTest {

    @Test
    void parsesSingleHunk() throws Exception {
        String diff = Files.readString(Path.of("src/test/resources/sample-diff/simple.diff"));
        List<DiffHunk> hunks = new PRDiffParser().parse(diff);
        assertThat(hunks).hasSize(1);
        DiffHunk h = hunks.get(0);
        assertThat(h.filePath()).isEqualTo("src/main/java/Foo.java");
        assertThat(h.addedLines()).containsExactly(42, 43, 44);
        assertThat(h.removedLines()).containsExactly(41);
    }

    @Test
    void parsesMultiFileMultiHunk() throws Exception {
        String diff = Files.readString(Path.of("src/test/resources/sample-diff/multi-hunk.diff"));
        List<DiffHunk> hunks = new PRDiffParser().parse(diff);
        // 2 files x 2 hunks -> 4 hunks
        assertThat(hunks).hasSize(4);
    }

    @Test
    void ignoresBinaryDiff() throws Exception {
        String diff = Files.readString(Path.of("src/test/resources/sample-diff/binary.diff"));
        List<DiffHunk> hunks = new PRDiffParser().parse(diff);
        assertThat(hunks).isEmpty();
    }
}
