package com.codemate.review.core.config;

import com.codemate.review.core.enums.Severity;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class CodemateConfigLoader {

    private final YAMLMapper mapper = new YAMLMapper();

    public CodemateConfig loadFromClasspath(String resource) {
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            if (in == null) return CodemateConfig.defaults();
            String s = new String(in.readAllBytes());
            return loadFromString(s);
        } catch (IOException e) {
            return CodemateConfig.defaults();
        }
    }

    @SuppressWarnings("unchecked")
    public CodemateConfig loadFromString(String yaml) {
        if (yaml == null || yaml.isBlank()) return CodemateConfig.defaults();
        try {
            Map<String, Object> root = mapper.readValue(yaml, Map.class);
            if (root == null || root.isEmpty()) return CodemateConfig.defaults();
            CodemateConfig d = CodemateConfig.defaults();
            int version = intOr(root.get("version"), d.version());
            CodemateConfig.Agents agents = mergeAgents(d.agents(), (Map<String, Object>) root.get("agents"));
            Severity minSev = root.containsKey("min_severity")
                    ? Severity.fromConfig(String.valueOf(root.get("min_severity")))
                    : d.minSeverity();
            int maxComments = intOr(root.get("max_comments_per_pr"), d.maxCommentsPerPr());
            int minConf = intOr(root.get("min_confidence"), d.minConfidence());
            List<String> exclude = listOr(root.get("exclude"), d.exclude());
            List<String> rules = listOr(root.get("custom_rules"), d.customRules());
            CodemateConfig.Llm llm = mergeLlm(d.llm(), (Map<String, Object>) root.get("llm"));
            return new CodemateConfig(version, agents, minSev, maxComments, minConf, exclude, rules, llm);
        } catch (IOException e) {
            return CodemateConfig.defaults();
        }
    }

    private static int intOr(Object v, int dflt) {
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s && !s.isBlank()) return Integer.parseInt(s.trim());
        return dflt;
    }

    private static List<String> listOr(Object v, List<String> dflt) {
        if (v instanceof List<?> l) return l.stream().map(String::valueOf).toList();
        return dflt;
    }

    private static CodemateConfig.Agents mergeAgents(CodemateConfig.Agents d, Map<String, Object> m) {
        if (m == null) return d;
        return new CodemateConfig.Agents(
                boolOr(m.get("bug"), d.bug()),
                boolOr(m.get("security"), d.security()),
                boolOr(m.get("performance"), d.performance()),
                boolOr(m.get("style"), d.style()),
                boolOr(m.get("design"), d.design())
        );
    }

    private static boolean boolOr(Object v, boolean dflt) {
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return dflt;
    }

    private static CodemateConfig.Llm mergeLlm(CodemateConfig.Llm d, Map<String, Object> m) {
        if (m == null) return d;
        String provider = m.get("provider") instanceof String s ? s : d.provider();
        String model = m.get("model") instanceof String s ? s : d.model();
        int maxTokens = intOr(m.get("max_tokens_per_review"), d.maxTokensPerReview());
        return new CodemateConfig.Llm(provider, model, maxTokens);
    }
}
