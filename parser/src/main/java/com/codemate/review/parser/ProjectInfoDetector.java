package com.codemate.review.parser;

import com.codemate.review.core.model.ProjectInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProjectInfoDetector {
    public ProjectInfo detect(Map<String, String> rootFiles) {
        if (rootFiles == null || rootFiles.isEmpty()) {
            return ProjectInfo.builder()
                    .buildTool("unknown")
                    .frameworks(List.of())
                    .languageVersion("")
                    .sizeLoc(0)
                    .build();
        }

        String buildTool;
        if (rootFiles.containsKey("pom.xml")) {
            buildTool = "maven";
        } else if (rootFiles.keySet().stream().anyMatch(k -> k.endsWith(".gradle") || k.endsWith(".gradle.kts"))) {
            buildTool = "gradle";
        } else {
            buildTool = "unknown";
        }

        String content = String.join("\n", rootFiles.values());
        List<String> frameworks = new ArrayList<>();
        if (content.contains("spring-boot")) frameworks.add("spring-boot");
        if (content.contains("mybatis")) frameworks.add("mybatis");
        if (content.contains("hibernate")) frameworks.add("hibernate");
        if (content.contains("junit")) frameworks.add("junit");

        return ProjectInfo.builder()
                .buildTool(buildTool)
                .frameworks(frameworks)
                .languageVersion("")
                .sizeLoc(0)
                .build();
    }
}
