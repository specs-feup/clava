/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.utils.*;
import pt.up.fe.specs.clava.utils.DefinitionResolution.Kind;

/** Physical source ownership, coordinated edits and output checked by an independent C++ compiler. */
class UsrSourceOwnershipRegressionTest extends CrossTuTestSupport {
    private Path emitAndCompile(App app) throws Exception {
        Path output = Files.createTempDirectory(temporary, "generated-");
        var files = app.write(output.toFile());
        for (var file : files) if (file.getName().endsWith(".cpp")) {
            var command = new ArrayList<>(List.of("g++", "-std=c++20", "-I" + output));
            for (var unit : app.getTranslationUnits()) unit.get(pt.up.fe.specs.clava.ast.extra.TranslationUnit.SOURCE_FOLDERNAME)
                    .ifPresent(folder -> command.add("-I" + output.resolve(folder)));
            command.addAll(List.of("-c", file.toString(), "-o", output.resolve(file.getName() + ".o").toString()));
            var process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String diagnostics = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(0, process.waitFor(), diagnostics);
        }
        return output;
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void oneRenameCoordinatesStaticHeaderEntitiesAndEmitsOneHeader(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "static int helper(){return 7;}\nstatic int wrapper(){return helper();}\n",
                "a.cpp", "#include \"api.hpp\"\nint a(){return wrapper()+helper();}",
                "b.cpp", "#include \"api.hpp\"\nint b(){return wrapper()+helper();}"));
        var calls = app.getDescendants(CallExpr.class).stream().filter(c -> c.getCode().startsWith("helper(")).toList();
        var first = calls.get(0).getFunctionDecl().orElseThrow();
        var second = calls.get(1).getFunctionDecl().orElseThrow();
        assertFalse(app.isSameFunctionEntity(first, second));
        var canonical = calls.get(0).getDefinition().orElseThrow();
        assertSame(canonical, calls.get(1).getDefinition().orElseThrow());
        assertEquals(3, canonical.getCalls().size());
        canonical.setName("changed");
        assertEquals("changed", first.getDeclName());
        assertEquals("changed", second.getDeclName());
        assertFalse(app.isSameFunctionEntity(first, second), "Shared source must not merge TU-local semantic entities");
        assertNotEquals(first.get(NamedDecl.EDITED_ENTITY), second.get(NamedDecl.EDITED_ENTITY));
        for (var call : calls) assertTrue(call.getCode().startsWith("changed("));
        for (var unit : app.getTranslationUnits()) assertFalse(unit.getCode().contains("static int changed"));
        Path output = emitAndCompile(app);
        String header = Files.readString(output.resolve("api.hpp"));
        assertEquals(1, header.split("static int changed", -1).length - 1);
        assertTrue(header.contains("return changed();"));
        assertTrue(header.contains("static int wrapper"));
        assertEquals(3, app.getCode(output.toFile()).size());
        assertEquals(3, canonical.getCalls().size());
        var filtered = app.getCode(output.toFile(), Set.of());
        for (var file : filtered.entrySet()) if (file.getKey().getName().endsWith(".cpp"))
            assertTrue(file.getValue().contains("changed()"), file.getValue());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void sourceEditsPreserveMacrosAcrossDifferentContextsAndSuccessiveActions(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "// Keep this source text\nstatic int helper(){return VALUE;}\n",
                "a.cpp", "#define VALUE 1\n#include \"api.hpp\"\nint a(){return helper();}",
                "b.cpp", "#define VALUE 2\n#include \"api.hpp\"\nint b(){return helper();}"));
        var calls = app.getDescendants(CallExpr.class);
        var definition = calls.get(0).getDefinition().orElseThrow();
        var contexts = app.getFunctionDefinitionResolution(definition).contextualDefinitions();
        assertEquals(2, contexts.size());
        assertNotEquals(contexts.get(0).getCode(), contexts.get(1).getCode());
        definition.setName("renamed");
        definition.setName("longer_name");
        definition.setReturnType(app.getFactory().builtinType("long"));
        Path output = emitAndCompile(app);
        String header = Files.readString(output.resolve("api.hpp"));
        assertTrue(header.contains("// Keep this source text"));
        assertTrue(header.contains("static long longer_name(){return VALUE;}"), header);
        assertFalse(header.contains("return 1"));
        assertFalse(header.contains("return 2"));
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void methodRenameUpdatesMemberCallersInSourceAndHeader(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "struct R{int method(){return 7;} int wrapper(){return method();}};\n",
                "a.cpp", "#include \"api.hpp\"\nint a(){R r;return r.method();}",
                "b.cpp", "#include \"api.hpp\"\nint b(){R r;return r.method();}"));
        var definition = app.getDescendants(CallExpr.class).get(0).getDefinition().orElseThrow();
        definition.setName("changed");
        for (var unit : app.getTranslationUnits()) assertTrue(unit.getCode().contains("r.changed()"), unit.getCode());
        Path output = emitAndCompile(app);
        String header = Files.readString(output.resolve("api.hpp"));
        assertTrue(header.contains("int changed()"), header);
        assertTrue(header.contains("return this->changed();") || header.contains("return changed();"), header);
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void changedSourceRequiresRefreshBeforeAnyOccurrenceChanges(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "static int helper(){return 7;}",
                "a.cpp", "#include \"api.hpp\"\nint a(){return helper();}",
                "b.cpp", "#include \"api.hpp\"\nint b(){return helper();}"));
        var definition = app.getDescendants(CallExpr.class).get(0).getDefinition().orElseThrow();
        Path source = Path.of(definition.getLocation().getFilepath());
        Files.writeString(source, "static int helper(){return 8;}");
        assertThrows(RefreshRequiredException.class, () -> definition.setName("changed"));
        assertEquals("helper", definition.getDeclName());
        for (var call : app.getDescendants(CallExpr.class)) assertEquals("helper()", call.getCode());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void unsafeMacroSignatureEditRollsBackEveryOccurrence(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "static RESULT helper(){return 7;}",
                "a.cpp", "#define RESULT int\n#include \"api.hpp\"\nint a(){return helper();}",
                "b.cpp", "#define RESULT long\n#include \"api.hpp\"\nint b(){return helper();}"));
        var definition = app.getDescendants(CallExpr.class).get(0).getDefinition().orElseThrow();
        String before = definition.getCode();
        assertThrows(RefreshRequiredException.class, () -> definition.setReturnType(app.getFactory().builtinType("double")));
        assertEquals(before, definition.getCode());
        assertEquals(Kind.UNIQUE, app.getFunctionDefinitionResolution(definition).kind());
        assertTrue(app.getDescendants(CallExpr.class).stream().allMatch(call -> call.getDefinition().isPresent()));
        assertEquals(2, app.getCode(temporary.resolve("unchanged").toFile()).size());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void generatedParameterizedPrototypeMatchesWithoutAcquiringAUsr(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int use(){return 1;}", "b.cpp", "int target(int x){return x;}"));
        var definition = function(app, "target", "b.cpp");
        var generated = app.getFactory().functionDecl("target", definition.getType().copy());
        generated.setParameters(List.of(app.getFactory().parmVarDecl("x", app.getFactory().builtinType("int"))));
        app.getTranslationUnits().get(0).addChild(generated);
        app.clearCache();
        assertSame(definition, generated.getImplementation().orElseThrow());
        assertTrue(app.isSameFunctionEntity(generated, definition));
        assertFalse(generated.hasValue(NamedDecl.USR));
        definition.setName("changed");
        assertEquals("changed", generated.getDeclName());
        assertSame(definition, generated.getImplementation().orElseThrow());
        emitAndCompile(app);
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void generatedPrototypeDoesNotChooseBetweenConflictingSources(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int target(){return 1;}", "b.cpp", "int target(){return 2;}"));
        var generated = app.getFactory().functionDecl("target", function(app, "target", "a.cpp").getType());
        app.getTranslationUnits().get(0).addChild(generated);
        app.clearCache();
        assertEquals(Kind.AMBIGUOUS, app.getFunctionDefinitionResolution(generated).kind());
        assertTrue(generated.getImplementation().isEmpty());
    }
    @ParameterizedTest @EnumSource(Mode.class)
    void parameterRollbackRestoresOriginalChildObjectsAndTypes(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "static int helper(PARAM x){return 7;}",
                "a.cpp", "#define PARAM int\n#include \"api.hpp\"\nint a(){return helper(1);}",
                "b.cpp", "#define PARAM double\n#include \"api.hpp\"\nint b(){return helper(1);}"));
        var calls = app.getDescendants(CallExpr.class);
        var declarations = calls.stream().map(call -> call.getFunctionDecl().orElseThrow()).toList();
        var parameters = declarations.stream().map(declaration -> declaration.getParameters().get(0)).toList();
        var codes = declarations.stream().map(FunctionDecl::getCode).toList();
        assertThrows(RefreshRequiredException.class,
                () -> calls.get(0).getDefinition().orElseThrow().setParamType(0, app.getFactory().builtinType("long")));
        for (int i = 0; i < declarations.size(); i++) {
            assertSame(parameters.get(i), declarations.get(i).getParameters().get(0));
            assertEquals(codes.get(i), declarations.get(i).getCode());
            assertTrue(CompilerEntityIdentity.isCurrent(declarations.get(i)));
            assertTrue(declarations.get(i).get(NamedDecl.EDITED_ENTITY).isEmpty());
        }
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void distinctHeadersWithTheSameBasenameRemainDistinctSourceDefinitions(Mode mode) throws Exception {
        var app = parse(mode, sources("first/api.hpp", "inline int target(){return 1;}",
                "second/api.hpp", "inline int target(){return 2;}",
                "a.cpp", "#include \"first/api.hpp\"\nint a(){return target();}",
                "b.cpp", "#include \"second/api.hpp\"\nint b(){return target();}"));
        var definition = app.getDescendants(CallExpr.class).get(0).getFunctionDecl().orElseThrow();
        var result = app.getFunctionDefinitionResolution(definition);
        assertEquals(Kind.AMBIGUOUS, result.kind());
        assertEquals(2, result.candidates().size());
        assertNotEquals(SourceDeclaration.of(result.candidates().get(0)), SourceDeclaration.of(result.candidates().get(1)));
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void staleUnqueriedContextCannotBeHiddenBySourceCanonicalization(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "static int helper(){return 7;}",
                "a.cpp", "#include \"api.hpp\"\nint a(){return helper();}",
                "b.cpp", "#include \"api.hpp\"\nint b(){return helper();}"));
        var calls = app.getDescendants(CallExpr.class);
        var query = calls.get(0).getFunctionDecl().orElseThrow();
        assertEquals(Kind.UNIQUE, app.getFunctionDefinitionResolution(query).kind());
        calls.get(1).getFunctionDecl().orElseThrow().set(NamedDecl.DECL_NAME, "raw_change");
        assertEquals(Kind.STALE, app.getFunctionDefinitionResolution(query).kind());
        assertThrows(IllegalStateException.class, () -> query.setName("changed"));
        assertEquals("helper", query.getDeclName());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void incompatibleGeneratedPrototypeCannotAdoptParsedRelationships(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int target(){return 1;}", "b.cpp", "int other(){return 2;}"));
        var definition = function(app, "target", "a.cpp");
        var generated = app.getFactory().functionDecl("target", definition.getType().copy());
        generated.getFunctionType().set(pt.up.fe.specs.clava.ast.type.FunctionType.RETURN_TYPE,
                app.getFactory().builtinType("double"));
        app.getTranslationUnits().get(0).addChild(generated);
        app.clearCache();
        assertEquals(Kind.INCOMPATIBLE, app.getFunctionDefinitionResolution(generated).kind());
        assertThrows(IllegalStateException.class, () -> generated.setName("changed"));
        assertEquals("target", definition.getDeclName());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void bodyActionUpdatesAllViewsAndOneSourceDefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "inline int target(){return 7;}",
                "a.cpp", "#include \"api.hpp\"\nint a(){return target();}",
                "b.cpp", "#include \"api.hpp\"\nint b(){return target();}",
                "replacement.cpp", "int replacement(){return 42;}"));
        var definition = app.getDescendants(CallExpr.class).get(0).getDefinition().orElseThrow();
        definition.setBody(function(app, "replacement", "replacement.cpp").getBody().orElseThrow());
        for (var context : app.getFunctionDefinitionResolution(definition).contextualDefinitions())
            assertTrue(context.getBody().orElseThrow().getCode().contains("return 42;"));
        Path output = emitAndCompile(app);
        assertTrue(Files.readString(output.resolve("api.hpp")).contains("return 42;"));
        for (var unit : app.getTranslationUnits()) if (!unit.getFilename().equals("replacement.cpp"))
            assertFalse(unit.getCode().contains("return 42;"));
    }

    @org.junit.jupiter.api.Test
    void selectedEditableHeaderOwnsTheCanonicalDefinitionAndIsEmittedOnce() throws Exception {
        Path root = Files.createDirectory(temporary.resolve("selected"));
        Path header = root.resolve("api.hpp"), source = root.resolve("use.cpp");
        Files.writeString(header, "inline int target(){return 7;}");
        Files.writeString(source, "#include \"api.hpp\"\nint use(){return target();}");
        var parser = pt.up.fe.specs.clang.codeparser.CodeParser.newInstance();
        parser.set(pt.up.fe.specs.clang.codeparser.CodeParser.GENERATED_PARSE_ROOT, root.toFile());
        parser.set(pt.up.fe.specs.clang.codeparser.CodeParser.AST_DUMP_CACHE, false);
        var app = parser.parse(List.of(header.toFile(), source.toFile()), List.of("-std=c++20", "-I" + root));
        var owner = function(app, "target", "api.hpp");
        var call = app.getDescendants(CallExpr.class).get(0);
        assertSame(owner, call.getDefinition().orElseThrow());
        owner.setName("changed");
        assertEquals("changed()", call.getCode());
        Path output = emitAndCompile(app);
        assertEquals(2, app.getCode(output.toFile()).size());
        assertTrue(Files.readString(output.resolve("api.hpp")).contains("int changed()"));
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void parameterReplacementRebindsBodyReferencesAcrossHeaderContexts(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "inline int target(int old_name){return old_name;}",
                "a.cpp", "#include \"api.hpp\"\nint a(){return target(1);}",
                "b.cpp", "#include \"api.hpp\"\nint b(){return target(2);}"));
        var definition = app.getDescendants(CallExpr.class).get(0).getDefinition().orElseThrow();
        definition.setParameters(List.of(app.getFactory().parmVarDecl("new_name", app.getFactory().builtinType("int"))));
        for (var context : app.getFunctionDefinitionResolution(definition).contextualDefinitions()) {
            var reference = context.getDescendants(pt.up.fe.specs.clava.ast.expr.DeclRefExpr.class).get(0);
            assertSame(context.getParameters().get(0), reference.getDeclaration());
        }
        Path output = emitAndCompile(app);
        String header = Files.readString(output.resolve("api.hpp"));
        assertTrue(header.contains("new_name"));
        assertFalse(header.contains("old_name"));
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void stagedArityChangeRequiresCallerUpdatesBeforeEmission(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "inline int target(){return 7;}",
                "a.cpp", "#include \"api.hpp\"\nint a(){return target();}",
                "b.cpp", "#include \"api.hpp\"\nint b(){return target();}"));
        var definition = app.getDescendants(CallExpr.class).get(0).getDefinition().orElseThrow();
        definition.setParameters(List.of(app.getFactory().parmVarDecl("new_argument", app.getFactory().builtinType("int"))));
        for (var context : app.getFunctionDefinitionResolution(definition).contextualDefinitions())
            assertEquals(1, context.getNumParameters());
        assertThrows(RefreshRequiredException.class, () -> app.getCode(temporary.resolve("pending-arity").toFile()));
        for (var call : app.getDescendants(CallExpr.class)) call.addArgument("1", app.getFactory().builtinType("int"));
        emitAndCompile(app);
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void configuredOutputPathsKeepHeaderWithItsSourceRoot(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "inline int target(){return 7;}",
                "a.cpp", "#include \"api.hpp\"\nint a(){return target();}",
                "b.cpp", "#include \"api.hpp\"\nint b(){return target();}"));
        for (var unit : app.getTranslationUnits()) {
            unit.setRelativePath(unit.getFile().getParentFile().getName());
            unit.set(pt.up.fe.specs.clava.ast.extra.TranslationUnit.SOURCE_FOLDERNAME, Optional.of("project"));
        }
        app.getDescendants(CallExpr.class).get(0).getDefinition().orElseThrow().setName("changed");
        Path output = emitAndCompile(app);
        assertTrue(Files.isRegularFile(output.resolve("project/api.hpp")));
        assertFalse(Files.exists(output.resolve("api.hpp")));
        assertEquals(3, app.getCode(output.toFile()).size());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void templateInstantiationsResolveToTheWrittenHeaderTemplate(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "template<typename T> T target(T x){return x;}",
                "a.cpp", "#include \"api.hpp\"\nint a(){return target(1);}",
                "b.cpp", "#include \"api.hpp\"\ndouble b(){return target(2.0);}"));
        var calls = app.getDescendants(CallExpr.class);
        var definition = calls.get(0).getDefinition().orElseThrow();
        assertEquals(pt.up.fe.specs.clava.ast.decl.enums.TemplateKind.FUNCTION_TEMPLATE,
                definition.get(FunctionDecl.TEMPLATE_KIND));
        assertSame(definition, calls.get(1).getDefinition().orElseThrow());
        definition.setName("changed");
        for (var call : calls) assertTrue(call.getCode().startsWith("changed("), call.getCode());
        Path output = emitAndCompile(app);
        assertEquals(1, Files.readString(output.resolve("api.hpp")).split("changed", -1).length - 1);
    }

}
