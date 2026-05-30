package com.codemate.review.core.enums;

public enum Severity {
    LOW(1), MEDIUM(2), HIGH(3), CRITICAL(4);

    private final int rank;

    Severity(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public static Severity fromConfig(String s) {
        return Severity.valueOf(s.toUpperCase());
    }

    public boolean atLeast(Severity other) {
        return this.rank >= other.rank;
    }
}
