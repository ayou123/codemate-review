package com.codemate.review.agent.prompt;

import com.codemate.review.core.model.ChangedMethod;
import com.codemate.review.core.model.PRContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PromptTemplates {

    public String render(String agentName, ChangedMethod method, PRContext ctx, String projectReferences) {
        String tpl = loadResourceOrEmpty("/prompts/" + agentName + ".md");
        if (tpl.isEmpty()) return "";
        String outputFmt = loadResourceOrEmpty("/prompts/common-output-format.md");

        var pi = ctx == null ? null : ctx.projectInfo();
        var cfg = ctx == null ? null : ctx.config();

        Map<String, String> vars = new HashMap<>();
        vars.put("filePath", n(method.filePath()));
        vars.put("className", n(method.className()));
        vars.put("methodName", n(method.methodName()));
        vars.put("fullCode", n(method.fullCode()));
        vars.put("diffCode", n(method.diffCode()));
        vars.put("projectBuildTool", pi == null ? "" : n(pi.buildTool()));
        vars.put("projectFrameworks", pi == null ? "" : String.join(",", pi.frameworks()));
        vars.put("customRules", cfg == null ? "" : String.join("\n- ", cfg.customRules()));
        vars.put("outputFormat", outputFmt);
        vars.put("projectReferences", projectReferences == null ? "" : projectReferences);

        return substitute(tpl, vars);
    }

    private static String n(String s) { return s == null ? "" : s; }

    private static String substitute(String tpl, Map<String, String> vars) {
        StringBuilder out = new StringBuilder(tpl.length());
        int i = 0;
        while (i < tpl.length()) {
            int open = tpl.indexOf("${", i);
            if (open < 0) { out.append(tpl, i, tpl.length()); break; }
            int close = tpl.indexOf("}", open + 2);
            if (close < 0) { out.append(tpl, i, tpl.length()); break; }
            out.append(tpl, i, open);
            String key = tpl.substring(open + 2, close);
            out.append(vars.getOrDefault(key, ""));
            i = close + 1;
        }
        return out.toString();
    }

    private String loadResourceOrEmpty(String path) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) return "";
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
