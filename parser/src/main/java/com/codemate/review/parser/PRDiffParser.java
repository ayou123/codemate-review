package com.codemate.review.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hand-rolled parser for GitHub unified diff text.
 *
 * <p>Splits raw diff text into {@link DiffHunk} records, one per hunk. The
 * new-side line number is tracked for added/context lines; the old-side line
 * number is tracked for removed/context lines, so {@code addedLines} are
 * indexed against the post-change file and {@code removedLines} against the
 * pre-change file.
 *
 * <p>Binary diffs (signalled by the {@code Binary files ... differ} marker)
 * produce no hunks.
 */
public class PRDiffParser {

    private static final Pattern FILE_HEADER =
            Pattern.compile("^diff --git a/(\\S+) b/(\\S+)$");

    private static final Pattern HUNK_HEADER =
            Pattern.compile("^@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@.*$");

    private static final String BINARY_MARKER = "Binary files";

    public List<DiffHunk> parse(String diff) {
        if (diff == null || diff.isBlank()) {
            return List.of();
        }
        List<DiffHunk> hunks = new ArrayList<>();
        String[] lines = diff.split("\\r?\\n", -1);
        String currentFile = null;
        boolean currentIsBinary = false;
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];

            Matcher fm = FILE_HEADER.matcher(line);
            if (fm.matches()) {
                // b/path is the new path — what downstream consumers expect.
                currentFile = fm.group(2);
                currentIsBinary = false;
                i++;
                continue;
            }
            if (line.startsWith(BINARY_MARKER)) {
                currentIsBinary = true;
                i++;
                continue;
            }

            Matcher hm = HUNK_HEADER.matcher(line);
            if (hm.matches() && currentFile != null && !currentIsBinary) {
                int oldStart = Integer.parseInt(hm.group(1));
                int newStart = Integer.parseInt(hm.group(3));
                int newCount = hm.group(4) == null ? 1 : Integer.parseInt(hm.group(4));

                StringBuilder hunkBody = new StringBuilder(line).append('\n');
                List<Integer> added = new ArrayList<>();
                List<Integer> removed = new ArrayList<>();
                int oldLine = oldStart;
                int newLine = newStart;
                int j = i + 1;
                while (j < lines.length
                        && !FILE_HEADER.matcher(lines[j]).matches()
                        && !HUNK_HEADER.matcher(lines[j]).matches()) {
                    String body = lines[j];
                    hunkBody.append(body).append('\n');
                    if (body.startsWith("+") && !body.startsWith("+++")) {
                        added.add(newLine);
                        newLine++;
                    } else if (body.startsWith("-") && !body.startsWith("---")) {
                        removed.add(oldLine);
                        oldLine++;
                    } else if (body.startsWith(" ")) {
                        oldLine++;
                        newLine++;
                    } else if (body.startsWith("\\")) {
                        // "\ No newline at end of file" — ignore.
                    }
                    j++;
                }
                hunks.add(new DiffHunk(currentFile, newStart, newCount, added, removed, hunkBody.toString()));
                i = j;
                continue;
            }
            i++;
        }
        return hunks;
    }
}
