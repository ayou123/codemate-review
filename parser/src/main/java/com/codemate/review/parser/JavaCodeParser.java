package com.codemate.review.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.List;

/**
 * Lightweight wrapper around JavaParser that extracts {@link MethodInfo}
 * records from a single Java source file. Does NOT enable SymbolSolver
 * (syntax-only parsing) so it tolerates undefined references.
 */
public class JavaCodeParser {

    private final JavaParser parser = new JavaParser();

    /**
     * Parse the given Java source and return one {@link MethodInfo} per
     * method declaration. Constructors are intentionally excluded.
     * <p>
     * On any parse failure (malformed Java, null/blank input) returns an
     * empty list rather than throwing.
     */
    public List<MethodInfo> parseMethods(String source, String filePath) {
        if (source == null || source.isBlank()) {
            return List.of();
        }
        var result = parser.parse(source);
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            return List.of();
        }
        CompilationUnit cu = result.getResult().get();
        return cu.findAll(MethodDeclaration.class).stream().map(m -> {
            String cls = m.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(c -> c.getNameAsString())
                .orElse("?");
            int s = m.getBegin().map(p -> p.line).orElse(-1);
            int e = m.getEnd().map(p -> p.line).orElse(-1);
            List<String> callees = m.findAll(MethodCallExpr.class).stream()
                .map(MethodCallExpr::getNameAsString)
                .distinct()
                .toList();
            return new MethodInfo(filePath, cls, m.getNameAsString(), s, e, m.toString(), callees);
        }).toList();
    }
}
