package com.codemate.review.parser;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JavaCodeParserTest {

    @Test
    void extractsMethodsWithLineRange() throws Exception {
        String src = Files.readString(Path.of("src/test/resources/sample-source/UserService.java"));
        var methods = new JavaCodeParser().parseMethods(src, "UserService.java");
        assertThat(methods).extracting(MethodInfo::methodName)
            .containsExactly("getUser", "saveUser");
        var getUser = methods.get(0);
        assertThat(getUser.className()).isEqualTo("UserService");
        assertThat(getUser.startLine()).isGreaterThanOrEqualTo(1);
        assertThat(getUser.endLine()).isGreaterThan(getUser.startLine());
        assertThat(getUser.fullCode()).contains("return repository.findById");
        assertThat(getUser.calleeMethodNames()).contains("findById");
    }

    @Test
    void handlesParseFailureGracefully() {
        assertThat(new JavaCodeParser().parseMethods("not valid java", "X.java")).isEmpty();
    }
}
