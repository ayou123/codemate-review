package com.codemate.review.core.model;

import com.codemate.review.core.enums.ChangeType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public record ChangedMethod(
        @JsonProperty("filePath") String filePath,
        @JsonProperty("className") String className,
        @JsonProperty("methodName") String methodName,
        @JsonProperty("fullCode") String fullCode,
        @JsonProperty("diffCode") String diffCode,
        @JsonProperty("type") ChangeType type,
        @JsonProperty("callers") List<String> callers,
        @JsonProperty("callees") List<String> callees,
        @JsonProperty("dependencies") List<String> dependencies,
        @JsonProperty("startLine") int startLine,
        @JsonProperty("endLine") int endLine
) {
    public ChangedMethod {
        callers = callers == null ? List.of() : List.copyOf(callers);
        callees = callees == null ? List.of() : List.copyOf(callees);
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String filePath;
        private String className;
        private String methodName;
        private String fullCode;
        private String diffCode;
        private ChangeType type;
        private List<String> callers = new ArrayList<>();
        private List<String> callees = new ArrayList<>();
        private List<String> dependencies = new ArrayList<>();
        private int startLine;
        private int endLine;

        public Builder filePath(String v) { this.filePath = v; return this; }
        public Builder className(String v) { this.className = v; return this; }
        public Builder methodName(String v) { this.methodName = v; return this; }
        public Builder fullCode(String v) { this.fullCode = v; return this; }
        public Builder diffCode(String v) { this.diffCode = v; return this; }
        public Builder type(ChangeType v) { this.type = v; return this; }
        public Builder callers(List<String> v) { this.callers = v; return this; }
        public Builder callees(List<String> v) { this.callees = v; return this; }
        public Builder dependencies(List<String> v) { this.dependencies = v; return this; }
        public Builder startLine(int v) { this.startLine = v; return this; }
        public Builder endLine(int v) { this.endLine = v; return this; }

        public ChangedMethod build() {
            return new ChangedMethod(filePath, className, methodName, fullCode, diffCode,
                    type, callers, callees, dependencies, startLine, endLine);
        }
    }
}
