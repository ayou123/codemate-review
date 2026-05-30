package com.codemate.review.core.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeverityTest {
    @Test
    void rankingAscendsFromLowToCritical() {
        assertThat(Severity.LOW.getRank()).isLessThan(Severity.MEDIUM.getRank());
        assertThat(Severity.MEDIUM.getRank()).isLessThan(Severity.HIGH.getRank());
        assertThat(Severity.HIGH.getRank()).isLessThan(Severity.CRITICAL.getRank());
    }

    @Test
    void parseFromConfigStringIsCaseInsensitive() {
        assertThat(Severity.fromConfig("high")).isEqualTo(Severity.HIGH);
        assertThat(Severity.fromConfig("CRITICAL")).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void atLeastComparesByRank() {
        assertThat(Severity.HIGH.atLeast(Severity.MEDIUM)).isTrue();
        assertThat(Severity.LOW.atLeast(Severity.HIGH)).isFalse();
    }
}
