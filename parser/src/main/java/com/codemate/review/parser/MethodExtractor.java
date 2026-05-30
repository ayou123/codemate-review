package com.codemate.review.parser;

import com.codemate.review.core.enums.ChangeType;
import com.codemate.review.core.model.ChangedMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps {@link DiffHunk} entries onto containing {@link MethodInfo} declarations
 * to produce {@link ChangedMethod} records — the review unit consumed by the
 * downstream agents.
 *
 * <p>Membership rule (MVP): a method is considered touched when at least one
 * <em>added</em> line (new-side) falls inside its {@code [startLine, endLine]}
 * range, OR a hunk that overlaps the method's new-side range carries any
 * removed lines (approximate signal — we don't have the old-side AST).
 *
 * <p>Classification:
 * <ul>
 *   <li>{@link ChangeType#ADDED} — added lines cover the whole method range
 *   and no removed lines are present.</li>
 *   <li>{@link ChangeType#MODIFIED} — otherwise (any partial overlap).</li>
 *   <li>{@link ChangeType#DELETED} — not detectable from new-side content; not
 *   emitted in MVP.</li>
 * </ul>
 */
public class MethodExtractor {
    private final JavaCodeParser parser;

    public MethodExtractor(JavaCodeParser parser) {
        this.parser = parser;
    }

    public List<ChangedMethod> extract(List<DiffHunk> hunks, Map<String, String> filesByPath) {
        Map<String, List<MethodInfo>> methodsByFile = new HashMap<>();
        List<ChangedMethod> out = new ArrayList<>();
        for (DiffHunk h : hunks) {
            String src = filesByPath.get(h.filePath());
            if (src == null) continue;
            List<MethodInfo> methods = methodsByFile.computeIfAbsent(h.filePath(),
                p -> parser.parseMethods(src, p));
            for (MethodInfo m : methods) {
                List<Integer> addedInside = h.addedLines().stream()
                    .filter(l -> l >= m.startLine() && l <= m.endLine())
                    .toList();
                boolean hunkOverlaps = hunkOverlapsMethod(h, m);
                boolean hasRemovedInOverlap = !h.removedLines().isEmpty() && hunkOverlaps;
                if (addedInside.isEmpty() && !hasRemovedInOverlap) continue;

                int methodLineCount = m.endLine() - m.startLine() + 1;
                ChangeType type;
                if (!addedInside.isEmpty()
                        && addedInside.size() == methodLineCount
                        && h.removedLines().isEmpty()) {
                    type = ChangeType.ADDED;
                } else {
                    type = ChangeType.MODIFIED;
                }

                ChangedMethod cm = ChangedMethod.builder()
                    .filePath(m.filePath())
                    .className(m.className())
                    .methodName(m.methodName())
                    .fullCode(m.fullCode())
                    .diffCode(extractDiffSlice(h, m))
                    .type(type)
                    .callees(m.calleeMethodNames())
                    .callers(List.of())
                    .dependencies(List.of())
                    .startLine(m.startLine())
                    .endLine(m.endLine())
                    .build();
                out.add(cm);
            }
        }
        return out;
    }

    private boolean hunkOverlapsMethod(DiffHunk h, MethodInfo m) {
        int hunkStart = h.newStartLine();
        int hunkEnd = h.newStartLine() + Math.max(0, h.newLineCount() - 1);
        return !(hunkEnd < m.startLine() || hunkStart > m.endLine());
    }

    private String extractDiffSlice(DiffHunk h, MethodInfo m) {
        StringBuilder sb = new StringBuilder();
        sb.append("// in method ").append(m.className()).append('.').append(m.methodName())
          .append(" (lines ").append(m.startLine()).append("-").append(m.endLine()).append(")\n");
        sb.append(h.hunkContent());
        return sb.toString();
    }
}
