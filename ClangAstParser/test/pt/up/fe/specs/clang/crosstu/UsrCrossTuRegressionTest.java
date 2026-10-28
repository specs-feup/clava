/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.utils.CompilerEntityIdentity;
import pt.up.fe.specs.clava.utils.DefinitionResolution.Kind;

/** Native USR transport and index contract, in both real TU orders and both scheduling modes. */
class UsrCrossTuRegressionTest extends CrossTuTestSupport {
    @ParameterizedTest @EnumSource(Mode.class)
    void compilerEntityIdentityDoesNotMergeOccurrences(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int target(int); int use(){return target(1);}",
                "impl.cpp", "int target(int x){return x;}"));
        var declaration = function(app, "target", "use.cpp");
        var definition = function(app, "target", "impl.cpp");
        assertFalse(declaration.get(NamedDecl.USR_GENERATION_FAILED));
        assertTrue(declaration.get(NamedDecl.USR).startsWith("c:"));
        assertEquals(declaration.get(NamedDecl.USR), definition.get(NamedDecl.USR));
        assertEquals(CompilerEntityIdentity.key(declaration), CompilerEntityIdentity.key(definition));
        assertNotEquals(declaration.getId(), definition.getId());
        assertNotSame(declaration, definition);
        assertSame(declaration, unique(app, CallExpr.class, call -> true).get(CallExpr.DIRECT_CALLEE).orElseThrow());
        assertSame(definition, declaration.getImplementation().orElseThrow());
        assertEquals("declaration", declaration.get(NamedDecl.USR_DEFINITION_KIND));
        assertEquals("definition", definition.get(NamedDecl.USR_DEFINITION_KIND));
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void allConflictingDefinitionsAreRetained(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int target();",
                "a.cpp", "int target(){return 1;}", "b.cpp", "int target(){return 2;}",
                "c.cpp", "int target(){return 3;}"));
        var resolution = app.getFunctionDefinitionResolution(function(app, "target", "use.cpp"));
        assertEquals(Kind.AMBIGUOUS, resolution.kind());
        assertEquals(3, resolution.candidates().size());
        assertThrows(UnsupportedOperationException.class, () -> resolution.candidates().clear());
        for (var candidate : resolution.candidates()) assertTrue(candidate.getImplementation().isEmpty());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void directRenameCannotServeStaleCompilerFacts(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int target();", "impl.cpp", "int target(){return 1;}"));
        var declaration = function(app, "target", "use.cpp");
        assertTrue(declaration.getImplementation().isPresent());
        function(app, "target", "impl.cpp").set(NamedDecl.DECL_NAME, "changed");
        assertEquals(Kind.STALE, app.getFunctionDefinitionResolution(declaration).kind());
        assertTrue(declaration.getImplementation().isEmpty());
        app.clearCache();
        assertEquals(Kind.STALE, app.getFunctionDefinitionResolution(declaration).kind(),
                "Clearing a Java index does not rerun Clang");
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void failedUsrGenerationDoesNotFallBackToNames(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int target();", "impl.cpp", "int target(){return 1;}"));
        var declaration = function(app, "target", "use.cpp");
        declaration.set(NamedDecl.USR, "");
        declaration.set(NamedDecl.USR_GENERATION_FAILED, true);
        assertEquals(Kind.UNSUPPORTED, app.getFunctionDefinitionResolution(declaration).kind());
        assertTrue(declaration.getImplementation().isEmpty());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void headerInternalEntitiesBelongToDifferentTus(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "static int helper(){return 7;}",
                "a.cpp", "#include \"api.hpp\"\nint a(){return helper();}",
                "b.cpp", "#include \"api.hpp\"\nint b(){return helper();}"));
        var calls = app.getDescendants(CallExpr.class);
        var first = calls.get(0).get(CallExpr.DIRECT_CALLEE).orElseThrow();
        var second = calls.get(1).get(CallExpr.DIRECT_CALLEE).orElseThrow();
        assertTrue(first.get(NamedDecl.USR_IS_TU_LOCAL));
        assertNotEquals(CompilerEntityIdentity.key(first), CompilerEntityIdentity.key(second));
        for (var call : calls) assertSame(call.get(CallExpr.DIRECT_CALLEE).orElseThrow(), call.getDefinition().orElseThrow());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void macroConfiguredHeaderDefinitionsAreAmbiguous(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "inline int target(){return VALUE;}",
                "a.cpp", "#define VALUE 1\n#include \"api.hpp\"\nint a(){return target();}",
                "b.cpp", "#define VALUE 2\n#include \"api.hpp\"\nint b(){return target();}"));
        var calls = app.getDescendants(CallExpr.class);
        var first = calls.get(0).get(CallExpr.DIRECT_CALLEE).orElseThrow();
        var second = calls.get(1).get(CallExpr.DIRECT_CALLEE).orElseThrow();
        assertEquals(first.get(NamedDecl.USR), second.get(NamedDecl.USR));
        assertNotEquals(first.getBody().orElseThrow().getCode(), second.getBody().orElseThrow().getCode());
        for (var call : calls) {
            assertEquals(Kind.AMBIGUOUS, app.getFunctionDefinitionResolution(call.getFunctionDecl().orElseThrow()).kind());
            assertTrue(call.getDefinition().isEmpty());
        }
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void repeatedHeaderDefinitionsRemainSeparateUntilEquivalenceIsEstablished(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "inline int target(){return 7;}",
                "a.cpp", "#include \"api.hpp\"\nint a(){return target();}",
                "b.cpp", "#include \"api.hpp\"\nint b(){return target();}"));
        var call = app.getDescendants(CallExpr.class).get(0);
        var resolution = app.getFunctionDefinitionResolution(call.getFunctionDecl().orElseThrow());
        assertEquals(Kind.AMBIGUOUS, resolution.kind());
        assertEquals(2, resolution.candidates().size());
        assertNotSame(resolution.candidates().get(0), resolution.candidates().get(1));
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void includedInlineBodiesRemainAnalysisOccurrencesRatherThanSourceCopies(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "inline int target(){return 7;}",
                "a.cpp", "#include \"api.hpp\"\nint a(){return target();}",
                "b.cpp", "#include \"api.hpp\"\nint b(){return target();}"));
        var calls = app.getDescendants(CallExpr.class);
        assertEquals(2, calls.size());
        var first = calls.get(0).get(CallExpr.DIRECT_CALLEE).orElseThrow();
        var second = calls.get(1).get(CallExpr.DIRECT_CALLEE).orElseThrow();
        assertNotSame(first, second);
        assertTrue(first.hasBody());
        assertTrue(second.hasBody());
        assertEquals("api.hpp", first.getLocation().getFilename());
        assertEquals(first.get(NamedDecl.USR), second.get(NamedDecl.USR));
        assertEquals(2, app.getCode(temporary.resolve("output").toFile()).size());
        for (var unit : app.getTranslationUnits()) {
            assertTrue(unit.get(TranslationUnit.INCLUDED_DECLARATIONS).stream()
                    .anyMatch(declaration -> declaration == first || declaration == second));
            assertTrue(unit.getCode().contains("#include \"api.hpp\""));
            assertTrue(unit.getCode().contains("target()"));
            assertFalse(unit.getCode().contains("inline int target"));
            assertFalse(unit.getCode().contains("return 7"));
        }
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void removingATuRemovesItsIncludedDefinitions(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "inline int target(){return 7;}",
                "a.cpp", "int target();", "b.cpp", "#include \"api.hpp\"\nint b(){return target();}"));
        var declaration = function(app, "target", "a.cpp");
        assertEquals(Kind.UNIQUE, app.getFunctionDefinitionResolution(declaration).kind());
        app.getTranslationUnits().stream().filter(tu -> tu.getFile().getName().equals("b.cpp"))
                .findFirst().orElseThrow().detach();
        app.clearCache();
        assertEquals(Kind.MISSING, app.getFunctionDefinitionResolution(declaration).kind());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void incompatibleCDeclarationsCannotSupplyDefinitions(Mode mode) throws Exception {
        var app = parse(mode, sources("use.c", "int target(int);", "impl.c", "double target(double x){return x;}"));
        var declaration = function(app, "target", "use.c");
        assertEquals(Kind.INCOMPATIBLE, app.getFunctionDefinitionResolution(declaration).kind());
        assertTrue(declaration.getImplementation().isEmpty());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void globalAndRecordConflictsHaveInspectableResults(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "struct R; extern int value;",
                "a.cpp", "struct R{int x;}; int value=1;",
                "b.cpp", "struct R{double x;}; int value=2;"));
        assertEquals(Kind.AMBIGUOUS, app.getGlobalVarDefinitionResolution(named(app, VarDecl.class, "value", "use.cpp")).kind());
        assertEquals(Kind.AMBIGUOUS, app.getTagDefinitionResolution(named(app, TagDecl.class, "R", "use.cpp")).kind());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void nativeTypeIdentityNormalizesParameterCv(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int target(const int);", "b.cpp", "int target(int x){return x;}"));
        var first = function(app, "target", "a.cpp");
        var second = function(app, "target", "b.cpp");
        assertEquals(first.get(NamedDecl.USR_TYPE), second.get(NamedDecl.USR_TYPE));
        assertFalse(first.get(NamedDecl.USR_TYPE_GENERATION_FAILED));
        assertEquals(first.get(NamedDecl.USR_LINKAGE_NAME), second.get(NamedDecl.USR_LINKAGE_NAME));
        assertSame(second, first.getImplementation().orElseThrow(() -> new AssertionError(
                app.getFunctionDefinitionResolution(first) + " current=" + CompilerEntityIdentity.isCurrent(first)
                + "," + CompilerEntityIdentity.isCurrent(second))));
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void compilerLinkageNamesDistinguishNoexceptPointerOverloads(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int target(int(*)()); int target(int(*)()noexcept);",
                "b.cpp", "int target(int(*)()){return 1;} int target(int(*)()noexcept){return 2;}"));
        var declarations = app.getDescendants(FunctionDecl.class).stream()
                .filter(f -> file(f).equals("a.cpp")).toList();
        assertEquals(2, declarations.size());
        assertNotEquals(declarations.get(0).get(NamedDecl.USR_LINKAGE_NAME), declarations.get(1).get(NamedDecl.USR_LINKAGE_NAME));
        assertNotSame(declarations.get(0).getImplementation().orElseThrow(), declarations.get(1).getImplementation().orElseThrow());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void generatedDeclarationsDoNotAcquireAParsedIdentityBySpelling(Mode mode) throws Exception {
        var app = parse(mode, sources("impl.cpp", "int target(){return 7;}"));
        var generated = app.getFactory().functionDecl("target", function(app, "target", "impl.cpp").getFunctionType());
        app.getTranslationUnits().get(0).addChild(generated);
        app.clearCache();
        assertEquals(Kind.MISSING, app.getFunctionDefinitionResolution(generated).kind());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void cTentativeDefinitionStatusIsTransported(Mode mode) throws Exception {
        var app = parse(mode, sources("a.c", "int value;", "b.c", "extern int value;"));
        assertEquals("tentative", named(app, VarDecl.class, "value", "a.c").get(NamedDecl.USR_DEFINITION_KIND));
        assertEquals("declaration", named(app, VarDecl.class, "value", "b.c").get(NamedDecl.USR_DEFINITION_KIND));
    }
    @ParameterizedTest @EnumSource(Mode.class)
    void tentativeDeclarationsInOneTuYieldToItsFullDefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("use.c", "extern int value;",
                "impl.c", "int value; int value; int value=7;"));
        var declaration = named(app, VarDecl.class, "value", "use.c");
        var result = app.getGlobalVarDefinitionResolution(declaration);
        assertEquals(Kind.UNIQUE, result.kind());
        assertTrue(result.unique().orElseThrow().hasInit());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void tentativeDefinitionsAcrossTusRemainExplicitlyAmbiguous(Mode mode) throws Exception {
        var app = parse(mode, sources("use.c", "extern int value;", "a.c", "int value;", "b.c", "int value;"));
        assertEquals(Kind.AMBIGUOUS,
                app.getGlobalVarDefinitionResolution(named(app, VarDecl.class, "value", "use.c")).kind());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void typedIntegralTemplateArgumentsUseCompilerIdentity(Mode mode) throws Exception {
        var app = parse(mode, sources("both.cpp", "template<auto N> int target();\n"
                + "template<> int target<1>(){return 101;}\n"
                + "template<> int target<1L>(){return 202;}\n"
                + "int use(){return target<1>()+target<1L>();}"));
        var calls = app.getDescendants(CallExpr.class);
        assertEquals(2, calls.size());
        var first = calls.get(0).getDefinition().orElseThrow();
        var second = calls.get(1).getDefinition().orElseThrow();
        assertNotSame(first, second);
        assertNotEquals(first.get(NamedDecl.USR), second.get(NamedDecl.USR));
        assertNotEquals(first.getBody().orElseThrow().getCode(), second.getBody().orElseThrow().getCode());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void writtenSpecializationsSurviveCodeGenerationAndReparse(Mode mode) throws Exception {
        var app = parse(mode, sources("both.cpp", "template<int N> int target();\n"
                + "template<> int target<1>(){return 101;}\n"
                + "template<> int target<2>(){return 202;}\n"
                + "int use(){return target<1>()+target<2>();}"));
        String code = app.getTranslationUnits().get(0).getCode();
        var output = temporary.resolve("specializations.cpp");
        java.nio.file.Files.writeString(output, code);
        var compiler = new ProcessBuilder("clang++", "-std=c++20", "-fsyntax-only", output.toString())
                .redirectErrorStream(true).start();
        String diagnostics = new String(compiler.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, compiler.waitFor(), () -> diagnostics + "\n" + code);
        var reparsed = parse(mode, sources("both.cpp", code));
        assertEquals(2, reparsed.getDescendants(FunctionDecl.class).stream()
                .filter(f -> f.get(FunctionDecl.IS_EXPLICIT_TEMPLATE_SPECIALIZATION) && f.hasBody()).count());
        var calls = reparsed.getDescendants(CallExpr.class);
        assertEquals(2, calls.size());
        assertNotSame(calls.get(0).getDefinition().orElseThrow(), calls.get(1).getDefinition().orElseThrow());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void editingAParameterInvalidatesItsParsedEntity(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int target(int);", "impl.cpp", "int target(int x){return x;}"));
        var declaration = function(app, "target", "use.cpp");
        assertEquals(Kind.UNIQUE, app.getFunctionDefinitionResolution(declaration).kind());
        declaration.getParameters().get(0).setType(app.getFactory().builtinType("double"));
        assertEquals(Kind.STALE, app.getFunctionDefinitionResolution(declaration).kind());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void supportedEntityEditsKeepRelationshipsWithoutRefreshingCompilerFacts(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int target(int); int use(){return target(1);}",
                "impl.cpp", "int target(int value){return value;}"));
        var declaration = function(app, "target", "use.cpp");
        var definition = function(app, "target", "impl.cpp");
        String originalUsr = declaration.get(NamedDecl.USR);
        declaration.setReturnType(app.getFactory().builtinType("double"));
        assertEquals("double", definition.getReturnType().getCode());
        assertEquals(originalUsr, declaration.get(NamedDecl.USR));
        assertFalse(CompilerEntityIdentity.isCurrent(declaration), "Original compiler metadata remains historical");
        assertTrue(CompilerEntityIdentity.isEditCurrent(declaration));
        assertEquals(declaration.get(NamedDecl.EDITED_ENTITY), definition.get(NamedDecl.EDITED_ENTITY));
        assertSame(definition, declaration.getImplementation().orElseThrow());
        declaration.setName("changed");
        assertEquals("changed", definition.getDeclName());
        assertSame(definition, declaration.getImplementation().orElseThrow());
        assertEquals(1, definition.getCalls().size());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void parameterEditsDoNotShareParentedParameterOccurrences(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int target(int);", "impl.cpp", "int target(int x){return x;}"));
        var declaration = function(app, "target", "use.cpp");
        var definition = function(app, "target", "impl.cpp");
        var parameter = app.getFactory().parmVarDecl("newValue", app.getFactory().builtinType("double"));
        declaration.setParameters(List.of(parameter));
        assertSame(definition, declaration.getImplementation().orElseThrow());
        assertNotSame(declaration.getParameters().get(0), definition.getParameters().get(0));
        assertSame(declaration, declaration.getParameters().get(0).getParent());
        assertSame(definition, definition.getParameters().get(0).getParent());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void rawEditsAfterAManagedEditAreStillStale(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int target(int);", "impl.cpp", "int target(int x){return x;}"));
        var declaration = function(app, "target", "use.cpp");
        declaration.setReturnType(app.getFactory().builtinType("double"));
        assertEquals(Kind.UNIQUE, app.getFunctionDefinitionResolution(declaration).kind());
        function(app, "target", "impl.cpp").set(NamedDecl.DECL_NAME, "rawChange");
        assertEquals(Kind.STALE, app.getFunctionDefinitionResolution(declaration).kind());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void entityWideRenamesDoNotSelectAConflictingDefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int target();", "a.cpp", "int target(){return 1;}",
                "b.cpp", "int target(){return 2;}"));
        var declaration = function(app, "target", "use.cpp");
        declaration.setName("changed");
        assertTrue(app.getDescendants(FunctionDecl.class).stream().allMatch(f -> f.getDeclName().equals("changed")));
        var result = app.getFunctionDefinitionResolution(declaration);
        assertEquals(Kind.AMBIGUOUS, result.kind());
        assertEquals(2, result.candidates().size());
        assertTrue(declaration.getImplementation().isEmpty());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void clonesHaveNewEntityIdentity(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int target(int);", "impl.cpp", "int target(int x){return x;}"));
        var definition = function(app, "target", "impl.cpp");
        var clone = definition.cloneAndInsert("cloned", true);
        app.clearCache();
        assertFalse(clone.hasValue(NamedDecl.USR));
        assertSame(clone, function(app, "cloned", "use.cpp").getImplementation().orElseThrow());
        clone.setReturnType(app.getFactory().builtinType("double"));
        assertEquals("int", definition.getReturnType().getCode());
        assertSame(definition, function(app, "target", "use.cpp").getImplementation().orElseThrow());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void supportedRecordRenamePreservesSeparateOccurrences(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "struct R{int x;}; R a;", "b.cpp", "struct R{int x;}; R b;"));
        var first = named(app, TagDecl.class, "R", "a.cpp");
        var second = named(app, TagDecl.class, "R", "b.cpp");
        assertEquals(2, app.getEntityOccurrences(first).size());
        first.renameEntity("Changed");
        assertEquals("Changed", second.getDeclName());
        assertNotSame(first, second);
        assertEquals(Kind.AMBIGUOUS, app.getTagDefinitionResolution(first).kind());
        for (var tu : app.getTranslationUnits()) assertTrue(tu.getCode().contains("Changed"));
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void supportedGlobalRenameMaintainsReferences(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "extern int value; int use(){return value;}", "b.cpp", "int value=7;"));
        var declaration = named(app, VarDecl.class, "value", "a.cpp");
        var definition = named(app, VarDecl.class, "value", "b.cpp");
        declaration.renameEntity("changed");
        assertEquals("changed", definition.getDeclName());
        assertSame(definition, declaration.getDefinition());
        assertTrue(app.getTranslationUnits().stream().filter(tu -> tu.getFile().getName().equals("a.cpp"))
                .findFirst().orElseThrow().getCode().contains("return changed;"));
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void anonymousNamespaceEntitiesKeepTheirTuOwnership(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "namespace {int helper(){return 1;}} int a(){return helper();}",
                "b.cpp", "namespace {int helper(){return 2;}} int b(){return helper();}"));
        var calls = app.getDescendants(CallExpr.class);
        assertEquals(2, calls.size());
        for (var call : calls) {
            var definition = call.getDefinition().orElseThrow();
            assertTrue(definition.get(NamedDecl.USR_IS_TU_LOCAL));
            assertEquals(file(call), file(definition));
        }
        assertNotSame(calls.get(0).getDefinition().orElseThrow(), calls.get(1).getDefinition().orElseThrow());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void differentCallingConventionsCannotSupplyADefinition(Mode mode) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(List.of("amd64", "x86_64").contains(System.getProperty("os.arch")),
                "This calling-convention fixture targets x86-64");
        var app = parse(mode, sources("a.cpp", "__attribute__((ms_abi)) int target(int);",
                "b.cpp", "int target(int x){return x;}"));
        var declaration = function(app, "target", "a.cpp");
        assertTrue(declaration.getImplementation().isEmpty());
    }

}
