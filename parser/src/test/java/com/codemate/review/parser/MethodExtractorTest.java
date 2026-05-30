package com.codemate.review.parser;

import com.codemate.review.core.enums.ChangeType;
import com.codemate.review.core.model.ChangedMethod;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MethodExtractorTest {
    private final MethodExtractor extractor = new MethodExtractor(new JavaCodeParser());

    @Test
    void mapsAddedLinesToContainingMethod() throws Exception {
        String src = Files.readString(Path.of("src/test/resources/sample-source/UserService.java"));
        var methods = new JavaCodeParser().parseMethods(src, "UserService.java");
        var getUser = methods.stream().filter(m -> m.methodName().equals("getUser")).findFirst().orElseThrow();

        int midLine = (getUser.startLine() + getUser.endLine()) / 2;
        DiffHunk hunk = new DiffHunk("UserService.java", midLine, 1,
            List.of(midLine), List.of(), "+ some change\n");

        List<ChangedMethod> result = extractor.extract(List.of(hunk), Map.of("UserService.java", src));
        assertThat(result).hasSize(1);
        ChangedMethod cm = result.get(0);
        assertThat(cm.methodName()).isEqualTo("getUser");
        assertThat(cm.className()).isEqualTo("UserService");
        assertThat(cm.type()).isEqualTo(ChangeType.MODIFIED);
        assertThat(cm.callees()).contains("findById");
        assertThat(cm.startLine()).isEqualTo(getUser.startLine());
        assertThat(cm.endLine()).isEqualTo(getUser.endLine());
    }

    @Test
    void linesOutsideAnyMethodAreIgnored() throws Exception {
        String src = Files.readString(Path.of("src/test/resources/sample-source/UserService.java"));
        DiffHunk hunk = new DiffHunk("UserService.java", 1, 1, List.of(1), List.of(), "+ x\n");
        assertThat(extractor.extract(List.of(hunk), Map.of("UserService.java", src))).isEmpty();
    }

    @Test
    void newlyAddedMethodMarkedAsADDED() throws Exception {
        String src = Files.readString(Path.of("src/test/resources/sample-source/UserService.java"));
        var methods = new JavaCodeParser().parseMethods(src, "UserService.java");
        var saveUser = methods.stream().filter(m -> m.methodName().equals("saveUser")).findFirst().orElseThrow();

        List<Integer> added = java.util.stream.IntStream
            .rangeClosed(saveUser.startLine(), saveUser.endLine())
            .boxed().toList();
        DiffHunk hunk = new DiffHunk("UserService.java", saveUser.startLine(), added.size(),
            added, List.of(), "+ ... whole method ... \n");

        var result = extractor.extract(List.of(hunk), Map.of("UserService.java", src));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).methodName()).isEqualTo("saveUser");
        assertThat(result.get(0).type()).isEqualTo(ChangeType.ADDED);
    }

    @Test
    void hunkForFileNotInFilesMapIsSkipped() {
        DiffHunk hunk = new DiffHunk("Missing.java", 1, 1, List.of(1), List.of(), "");
        assertThat(extractor.extract(List.of(hunk), Map.of())).isEmpty();
    }
}
