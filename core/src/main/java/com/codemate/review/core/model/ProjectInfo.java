package com.codemate.review.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public record ProjectInfo(
        @JsonProperty("buildTool") String buildTool,
        @JsonProperty("frameworks") List<String> frameworks,
        @JsonProperty("languageVersion") String languageVersion,
        @JsonProperty("sizeLoc") int sizeLoc
) {
    public ProjectInfo {
        frameworks = frameworks == null ? List.of() : List.copyOf(frameworks);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String buildTool;
        private List<String> frameworks = new ArrayList<>();
        private String languageVersion;
        private int sizeLoc;

        public Builder buildTool(String v) { this.buildTool = v; return this; }
        public Builder frameworks(List<String> v) { this.frameworks = v; return this; }
        public Builder languageVersion(String v) { this.languageVersion = v; return this; }
        public Builder sizeLoc(int v) { this.sizeLoc = v; return this; }

        public ProjectInfo build() {
            return new ProjectInfo(buildTool, frameworks, languageVersion, sizeLoc);
        }
    }
}
