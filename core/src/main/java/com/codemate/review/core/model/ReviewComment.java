package com.codemate.review.core.model;

import com.codemate.review.core.enums.ReviewCategory;
import com.codemate.review.core.enums.Severity;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public record ReviewComment(
        @JsonProperty("agentName") String agentName,
        @JsonProperty("filePath") String filePath,
        @JsonProperty("line") int line,
        @JsonProperty("severity") Severity severity,
        @JsonProperty("category") ReviewCategory category,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("suggestion") String suggestion,
        @JsonProperty("suggestedCode") String suggestedCode,
        @JsonProperty("confidence") int confidence,
        @JsonProperty("references") List<String> references
) {
    public ReviewComment {
        references = references == null ? List.of() : List.copyOf(references);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String agentName;
        private String filePath;
        private int line;
        private Severity severity;
        private ReviewCategory category;
        private String title;
        private String description;
        private String suggestion;
        private String suggestedCode;
        private int confidence;
        private List<String> references = new ArrayList<>();

        public Builder agentName(String v) { this.agentName = v; return this; }
        public Builder filePath(String v) { this.filePath = v; return this; }
        public Builder line(int v) { this.line = v; return this; }
        public Builder severity(Severity v) { this.severity = v; return this; }
        public Builder category(ReviewCategory v) { this.category = v; return this; }
        public Builder title(String v) { this.title = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder suggestion(String v) { this.suggestion = v; return this; }
        public Builder suggestedCode(String v) { this.suggestedCode = v; return this; }
        public Builder confidence(int v) { this.confidence = v; return this; }
        public Builder references(List<String> v) { this.references = v; return this; }

        public ReviewComment build() {
            return new ReviewComment(agentName, filePath, line, severity, category,
                    title, description, suggestion, suggestedCode, confidence, references);
        }
    }
}
