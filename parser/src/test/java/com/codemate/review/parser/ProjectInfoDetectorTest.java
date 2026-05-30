package com.codemate.review.parser;

import com.codemate.review.core.model.ProjectInfo;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectInfoDetectorTest {
    @Test
    void detectsSpringBootMavenProject() {
        Map<String, String> files = Map.of("pom.xml",
                "<project>...<artifactId>spring-boot-starter-web</artifactId>...<artifactId>mybatis-spring-boot-starter</artifactId>...</project>");
        ProjectInfo info = new ProjectInfoDetector().detect(files);
        assertThat(info.buildTool()).isEqualTo("maven");
        assertThat(info.frameworks()).contains("spring-boot", "mybatis");
    }

    @Test
    void detectsGradle() {
        Map<String, String> files = Map.of("build.gradle.kts",
                "implementation(\"org.springframework.boot:spring-boot-starter-web\")");
        var info = new ProjectInfoDetector().detect(files);
        assertThat(info.buildTool()).isEqualTo("gradle");
        assertThat(info.frameworks()).contains("spring-boot");
    }

    @Test
    void detectsPlainGradleGroovyDsl() {
        Map<String, String> files = Map.of("build.gradle",
                "dependencies { implementation 'org.hibernate:hibernate-core:6.5' }");
        var info = new ProjectInfoDetector().detect(files);
        assertThat(info.buildTool()).isEqualTo("gradle");
        assertThat(info.frameworks()).contains("hibernate");
    }

    @Test
    void unknownReturnsDefaults() {
        var info = new ProjectInfoDetector().detect(Map.of());
        assertThat(info.buildTool()).isEqualTo("unknown");
        assertThat(info.frameworks()).isEmpty();
    }

    @Test
    void nullMapReturnsDefaults() {
        var info = new ProjectInfoDetector().detect(null);
        assertThat(info.buildTool()).isEqualTo("unknown");
        assertThat(info.frameworks()).isEmpty();
    }
}
