/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.util.SpecsSystem;

abstract class CrossTuTestSupport {
    enum Mode {
        FORWARD(false, false), REVERSE(true, false), PARALLEL(false, true), PARALLEL_REVERSE(true, true);
        final boolean reverse;
        final boolean parallel;
        Mode(boolean reverse, boolean parallel) {
            this.reverse = reverse;
            this.parallel = parallel;
        }
    }

    @TempDir Path temporary;

    static Map<String, String> sources(String... pathsAndCode) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < pathsAndCode.length; i += 2) {
            result.put(pathsAndCode[i], pathsAndCode[i + 1]);
        }
        return result;
    }

    App parse(Mode mode, Map<String, String> sources) throws IOException {
        SpecsSystem.programStandardInit();
        Path root = Files.createTempDirectory(temporary, "project-");
        List<File> inputs = new ArrayList<>();
        long sourceCount = sources.keySet().stream().filter(CrossTuTestSupport::isSource).count();
        int sourceIndex = 0;
        for (var entry : sources.entrySet()) {
            boolean isSource = isSource(entry.getKey());
            // ParallelCodeParser sorts paths. Reversing its input list alone would not reverse TU order.
            int rank = mode.reverse ? (int) sourceCount - 1 - sourceIndex : sourceIndex;
            Path path = isSource ? root.resolve(String.format("%03d", rank)).resolve(entry.getKey())
                    : root.resolve(entry.getKey());
            Files.createDirectories(path.getParent());
            Files.writeString(path, entry.getValue());
            if (isSource) {
                inputs.add(path.toFile());
                sourceIndex++;
            }
        }
        if (mode.reverse) {
            Collections.reverse(inputs);
        }
        CodeParser parser = CodeParser.newInstance();
        parser.set(CodeParser.GENERATED_PARSE_ROOT, root.toFile());
        parser.set(CodeParser.AST_DUMP_CACHE, false);
        parser.set(CodeParser.SHOW_EXEC_INFO, false);
        parser.set(ParallelCodeParser.PARALLEL_PARSING, mode.parallel);
        parser.set(ParallelCodeParser.PARSING_NUM_THREADS, 2);
        var app = parser.parse(inputs, List.of(inputs.get(0).getName().endsWith(".c") ? "-std=c11" : "-std=c++20",
                "-I" + root));
        assertEquals(inputs.stream().map(File::getAbsolutePath).toList(),
                app.getTranslationUnits().stream().map(tu -> tu.getFile().getAbsolutePath()).toList(),
                "The fixture must exercise the requested TU order, without silently dropping parse failures");
        return app;
    }

    private static boolean isSource(String path) { return path.endsWith(".cpp") || path.endsWith(".c"); }

    static <T extends NamedDecl> T named(App app, Class<T> kind, String name, String file) {
        return unique(app, kind, node -> node.getDeclName().equals(name) && file(node).equals(file));
    }

    static <T extends ClavaNode> T unique(App app, Class<T> kind, Predicate<T> predicate) {
        List<T> matches = app.getDescendants(kind).stream().filter(predicate).toList();
        if (matches.size() != 1) {
            throw new AssertionError("Expected one " + kind.getSimpleName() + ", found " + matches.size());
        }
        return matches.get(0);
    }

    static String file(ClavaNode node) {
        return node.getAncestor(TranslationUnit.class).getFile().getName();
    }

    static FunctionDecl function(App app, String name, String file) {
        return named(app, FunctionDecl.class, name, file);
    }
}
