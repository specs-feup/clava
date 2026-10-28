/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.LibcMode;
import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.wire.WireSchema;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.utils.DefinitionResolution.Kind;
import pt.up.fe.specs.util.SpecsSystem;

/** Real ccache hits and dependency/configuration invalidation, without publishing a native release. */
class UsrCacheRegressionTest {
    @TempDir Path temporary;
    Path root;
    Path dumperCache;

    @BeforeEach void setup() throws Exception {
        assumeTrue(System.getProperty("os.name").toLowerCase().contains("linux"));
        assumeTrue(System.getenv("CCACHE_DISABLE") == null);
        try {
            assumeTrue(new ProcessBuilder("ccache", "--version").start().waitFor() == 0);
        } catch (java.io.IOException e) {
            assumeTrue(false, "ccache is not installed");
        }
        SpecsSystem.programStandardInit();
        root = Files.createDirectories(temporary.resolve("project"));
        dumperCache = Files.createDirectories(temporary.resolve("cache"));
    }

    private void write(String file, String code) throws Exception {
        var path = root.resolve(file);
        Files.createDirectories(path.getParent());
        Files.writeString(path, code);
    }

    private App parse(List<String> files, String... flags) {
        var parser = CodeParser.newInstance();
        parser.set(CodeParser.GENERATED_PARSE_ROOT, root.toFile());
        parser.set(CodeParser.DUMPER_FOLDER, dumperCache.toFile());
        parser.set(CodeParser.AST_DUMP_CACHE, true);
        parser.set(CodeParser.SHOW_EXEC_INFO, false);
        parser.set(ClangAstKeys.LIBC_CXX_MODE, LibcMode.SYSTEM);
        var arguments = new ArrayList<>(List.of("-std=c++20", "-I" + root));
        arguments.addAll(List.of(flags));
        var app = parser.parse(files.stream().map(root::resolve).map(Path::toFile).toList(), arguments);
        assertEquals(files.size(), app.getTranslationUnits().size(), "No failed TU may silently disappear");
        return app;
    }

    private FunctionDecl target(App app) {
        return app.getDescendants(FunctionDecl.class).stream()
                .filter(f -> f.getDeclName().equals("target") && !f.hasBody()).findFirst().orElseThrow();
    }

    private Map<String, Long> stats() throws Exception {
        var command = new ProcessBuilder("ccache", "--print-stats");
        command.environment().put("CCACHE_DIR", dumperCache.resolve("clang-dumper-ccache")
                .resolve("flatbuffers-v2-" + WireSchema.get().hash).toString());
        var process = command.start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor());
        var result = new HashMap<String, Long>();
        output.lines().forEach(line -> {
            var pair = line.split("\\s+");
            result.put(pair[0], Long.parseLong(pair[1]));
        });
        return result;
    }

    private long hits() throws Exception {
        var stats = stats();
        return stats.getOrDefault("direct_cache_hit", 0L) + stats.getOrDefault("preprocessed_cache_hit", 0L);
    }

    @Test void warmAndMixedFreshDumpsRetainOnlyCurrentDefinitions() throws Exception {
        write("use.cpp", "int target(int); int use(){return target(1);}");
        write("impl.cpp", "int target(int x){return x;}");
        var files = List.of("use.cpp", "impl.cpp");
        var cold = parse(files);
        String usr = target(cold).get(NamedDecl.USR);
        assertEquals(Kind.UNIQUE, cold.getFunctionDefinitionResolution(target(cold)).kind());
        long before = hits();
        var warm = parse(files);
        assertTrue(hits() >= before + 2, "Both unchanged TU dumps must hit ccache");
        assertEquals(usr, target(warm).get(NamedDecl.USR));
        assertEquals(Kind.UNIQUE, warm.getFunctionDefinitionResolution(target(warm)).kind());
        assertNotSame(target(cold), target(warm));
        before = hits();
        write("impl.cpp", "int renamed(int x){return x;}");
        var mixed = parse(files);
        assertTrue(hits() >= before + 1, "The unchanged caller must reuse its dump");
        assertEquals(Kind.MISSING, mixed.getFunctionDefinitionResolution(target(mixed)).kind());
    }

    @Test void addingAndRemovingTusDoesNotBakeDefinitionChoicesIntoCachedCallers() throws Exception {
        write("use.cpp", "int target();");
        write("a.cpp", "int target(){return 1;}");
        write("b.cpp", "int target(){return 2;}");
        var app = parse(List.of("use.cpp"));
        assertEquals(Kind.MISSING, app.getFunctionDefinitionResolution(target(app)).kind());
        app = parse(List.of("use.cpp", "a.cpp"));
        assertEquals(Kind.UNIQUE, app.getFunctionDefinitionResolution(target(app)).kind());
        app = parse(List.of("use.cpp", "a.cpp", "b.cpp"));
        assertEquals(Kind.AMBIGUOUS, app.getFunctionDefinitionResolution(target(app)).kind());
        app = parse(List.of("use.cpp"));
        assertEquals(Kind.MISSING, app.getFunctionDefinitionResolution(target(app)).kind());
        assertTrue(hits() >= 3);
    }

    @Test void changedSharedHeaderInvalidatesBothEntityTypes() throws Exception {
        write("api.hpp", "#define PARAM int\nint target(PARAM);");
        write("use.cpp", "#include \"api.hpp\"\nint use(){return target(1);}");
        write("impl.cpp", "#include \"api.hpp\"\nint target(PARAM x){return 7;}");
        var files = List.of("use.cpp", "impl.cpp");
        var first = parse(files);
        var firstTarget = first.getDescendants(FunctionDecl.class).stream()
                .filter(f -> f.getDeclName().equals("target")).findFirst().orElseThrow();
        String firstUsr = firstTarget.get(NamedDecl.USR);
        parse(files);
        long before = stats().get("cache_miss");
        write("api.hpp", "#define PARAM double\nint target(PARAM);");
        var changed = parse(files);
        var secondTarget = changed.getDescendants(FunctionDecl.class).stream()
                .filter(f -> f.getDeclName().equals("target")).findFirst().orElseThrow();
        assertNotEquals(firstUsr, secondTarget.get(NamedDecl.USR));
        assertTrue(stats().get("cache_miss") >= before + 2);
    }

    @Test void compilerFlagConfigurationsDoNotShareStaleIdentities() throws Exception {
        write("use.cpp", "int target(PARAM);");
        write("impl.cpp", "int target(PARAM x){return 7;}");
        var files = List.of("use.cpp", "impl.cpp");
        var ints = parse(files, "-DPARAM=int");
        var doubles = parse(files, "-DPARAM=double");
        assertNotEquals(target(ints).get(NamedDecl.USR), target(doubles).get(NamedDecl.USR));
        assertEquals(Kind.UNIQUE, doubles.getFunctionDefinitionResolution(target(doubles)).kind());
        long before = hits();
        parse(files, "-DPARAM=int");
        assertTrue(hits() >= before + 2);
    }

    @Test void relocatedCachedInternalEntitiesUseTheirNewTuOwnership() throws Exception {
        write("unit.cpp", "static int helper(){return 7;} int use(){return helper();}");
        var original = parse(List.of("unit.cpp"));
        var first = original.getDescendants(FunctionDecl.class).stream().filter(f -> f.getDeclName().equals("helper")).findFirst().orElseThrow();
        root = Files.createDirectories(temporary.resolve("relocated"));
        write("unit.cpp", "static int helper(){return 7;} int use(){return helper();}");
        long before = hits();
        var relocated = parse(List.of("unit.cpp"));
        var second = relocated.getDescendants(FunctionDecl.class).stream().filter(f -> f.getDeclName().equals("helper")).findFirst().orElseThrow();
        assertTrue(hits() > before, "Relative dump paths must permit cache reuse after relocation");
        assertEquals(first.get(NamedDecl.USR), second.get(NamedDecl.USR));
        assertNotEquals(pt.up.fe.specs.clava.utils.CompilerEntityIdentity.key(first),
                pt.up.fe.specs.clava.utils.CompilerEntityIdentity.key(second));
        assertTrue(second.getLocation().getStart().getFilepath().startsWith(root.toString()));
    }
}
