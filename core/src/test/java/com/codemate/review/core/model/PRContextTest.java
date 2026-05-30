package com.codemate.review.core.model;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PRContextTest {
    @Test
    void changedMethodsListIsDefensivelyCopied() {
        var list = new ArrayList<ChangedMethod>();
        var ctx = PRContext.builder().repoName("o/r").changedMethods(list).build();
        list.add(ChangedMethod.builder().methodName("foo").build());
        assertThat(ctx.changedMethods()).isEmpty();
    }
}
