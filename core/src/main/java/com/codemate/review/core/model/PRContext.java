package com.codemate.review.core.model;

import com.codemate.review.core.config.CodemateConfig;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public record PRContext(
        @JsonProperty("repoName") String repoName,
        @JsonProperty("prNumber") int prNumber,
        @JsonProperty("prTitle") String prTitle,
        @JsonProperty("prDescription") String prDescription,
        @JsonProperty("baseSha") String baseSha,
        @JsonProperty("headSha") String headSha,
        @JsonProperty("projectInfo") ProjectInfo projectInfo,
        @JsonProperty("changedFiles") List<ChangedFile> changedFiles,
        @JsonProperty("changedMethods") List<ChangedMethod> changedMethods,
        @JsonProperty("repoId") Long repoId,
        @JsonProperty("config") CodemateConfig config
) {
    public PRContext {
        changedFiles = changedFiles == null ? List.of() : List.copyOf(changedFiles);
        changedMethods = changedMethods == null ? List.of() : List.copyOf(changedMethods);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String repoName;
        private int prNumber;
        private String prTitle;
        private String prDescription;
        private String baseSha;
        private String headSha;
        private ProjectInfo projectInfo;
        private List<ChangedFile> changedFiles = new ArrayList<>();
        private List<ChangedMethod> changedMethods = new ArrayList<>();
        private Long repoId;
        private CodemateConfig config;

        public Builder repoName(String v) { this.repoName = v; return this; }
        public Builder prNumber(int v) { this.prNumber = v; return this; }
        public Builder prTitle(String v) { this.prTitle = v; return this; }
        public Builder prDescription(String v) { this.prDescription = v; return this; }
        public Builder baseSha(String v) { this.baseSha = v; return this; }
        public Builder headSha(String v) { this.headSha = v; return this; }
        public Builder projectInfo(ProjectInfo v) { this.projectInfo = v; return this; }
        public Builder changedFiles(List<ChangedFile> v) { this.changedFiles = v; return this; }
        public Builder changedMethods(List<ChangedMethod> v) { this.changedMethods = v; return this; }
        public Builder repoId(Long v) { this.repoId = v; return this; }
        public Builder config(CodemateConfig v) { this.config = v; return this; }

        public PRContext build() {
            return new PRContext(repoName, prNumber, prTitle, prDescription, baseSha, headSha,
                    projectInfo, changedFiles, changedMethods, repoId, config);
        }
    }
}
