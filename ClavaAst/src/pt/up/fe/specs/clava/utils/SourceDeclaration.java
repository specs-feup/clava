/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.utils;

import java.nio.file.Path;
import java.util.Optional;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;

/** Physical source ownership, separate from compiler entity and TU occurrence identity. */
public record SourceDeclaration(Path file, int line, int column, int endLine, int endColumn, String family) {
    /** One resolver per analysis/capture pass; canonicalize each path once, not once per declaration. */
    public static final class Resolver {
        private final java.util.Map<Path, Path> paths = new java.util.HashMap<>();
        public Optional<SourceDeclaration> of(NamedDecl declaration) {
            return source(declaration, path -> paths.computeIfAbsent(path, SourceDeclaration::canonical));
        }
    }
    private static Path canonical(Path path) {
        try { return path.toRealPath(); } catch (java.io.IOException ignored) { return path; }
    }
    public static Optional<SourceDeclaration> of(NamedDecl declaration) {
        return source(declaration, SourceDeclaration::canonical);
    }
    private static Optional<SourceDeclaration> source(NamedDecl declaration, java.util.function.UnaryOperator<Path> canonical) {
        // Copies/generated declarations can inherit locations without owning that original source.
        if (!declaration.hasValue(NamedDecl.USR) || declaration.get(pt.up.fe.specs.clava.ClavaNode.IS_MACRO)) return Optional.empty();
        var range = declaration.getLocation();
        if (!range.isValid() || range.getStartLine() < 1 || range.getStartCol() < 1
                || range.getEndLine() < 1 || range.getEndCol() < 1
                || range.getStartFilepath() == null
                || !range.getStartFilepath().equals(range.getEndFilepath())) return Optional.empty();
        Path file = Path.of(range.getStartFilepath()).toAbsolutePath().normalize();
        file = canonical.apply(file);
        return Optional.of(new SourceDeclaration(file, range.getStartLine(), range.getStartCol(),
                range.getEndLine(), range.getEndCol(), declaration.getClass().getName()));
    }
}
