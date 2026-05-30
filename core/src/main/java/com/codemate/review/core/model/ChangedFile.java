package com.codemate.review.core.model;

import com.codemate.review.core.enums.ChangeType;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ChangedFile(
        @JsonProperty("path") String path,
        @JsonProperty("changeType") ChangeType changeType,
        @JsonProperty("additions") int additions,
        @JsonProperty("deletions") int deletions
) {
    public ChangedFile {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String path;
        private ChangeType changeType;
        private int additions;
        private int deletions;

        public Builder path(String v) { this.path = v; return this; }
        public Builder changeType(ChangeType v) { this.changeType = v; return this; }
        public Builder additions(int v) { this.additions = v; return this; }
        public Builder deletions(int v) { this.deletions = v; return this; }

        public ChangedFile build() {
            return new ChangedFile(path, changeType, additions, deletions);
        }
    }
}
