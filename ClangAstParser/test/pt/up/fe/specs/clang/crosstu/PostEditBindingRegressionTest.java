/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.nio.file.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.decl.enums.*;
import pt.up.fe.specs.clava.ast.extra.*;
import pt.up.fe.specs.clava.language.TagKind;
import pt.up.fe.specs.clava.utils.*;
import pt.up.fe.specs.clava.utils.DefinitionResolution.Kind;

/** Live API relationships: assertions use declaration objects and an independent edit-history model. */
class PostEditBindingRegressionTest extends CrossTuTestSupport {
    @ParameterizedTest @EnumSource(Mode.class)
    void detachedGeneratedRecordCanBeRenamedBeforeInsertionWithoutEditingItsOrigin(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "struct Original{int x;}; Original value;"));
        var original = named(app, RecordDecl.class, "Original", "a.cpp");
        var clone = (RecordDecl) original.copy();
        CompilerEntityIdentity.makeGeneratedTree(clone);
        clone.renameEntity("Independent");
        assertEquals("Original", original.getDeclName());
        app.getTranslationUnits().get(0).addChild(clone);
        assertSame(original, app.getTagDefinitionResolution(original).unique().orElseThrow());
        assertSame(clone, app.getTagDefinitionResolution(clone).unique().orElseThrow());
    }
    @ParameterizedTest @EnumSource(Mode.class)
    void transformedHistoryCompilesLinksAndRunsWithIndependentCompiler(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int shared=1; int target(int x){return x;}",
                "b.cpp", "extern int shared; int target(int); int main(){return target(shared)==7?0:1;}"));
        var target = function(app, "target", "a.cpp");
        var callerUnit = function(app, "main", "b.cpp").getAncestor(TranslationUnit.class);
        var generated = prototype(app, target, callerUnit);
        generated.setName("changed");
        generated.setParamType(0, app.getFactory().builtinType("long"));
        target.setReturnType(app.getFactory().builtinType("long"));
        var global = named(app, VarDecl.class, "shared", "a.cpp");
        global.setType(app.getFactory().builtinType("long"));
        global.setDeclName("changed_global");
        global.setInit(app.getFactory().integerLiteral(7));
        assertSame(target, generated.getImplementation().orElseThrow());
        Path output = Files.createDirectory(temporary.resolve("emitted"));
        var files = app.write(output.toFile());
        var command = new ArrayList<>(List.of("g++", "-std=c++20"));
        for (var file : files) if (file.getName().endsWith(".cpp")) command.add(file.toString());
        Path executable = output.resolve("program");
        command.addAll(List.of("-o", executable.toString()));
        var compiler = new ProcessBuilder(command).redirectErrorStream(true).start();
        String diagnostics = new String(compiler.getInputStream().readAllBytes());
        assertEquals(0, compiler.waitFor(), diagnostics);
        var program = new ProcessBuilder(executable.toString()).redirectErrorStream(true).start();
        assertTrue(program.waitFor(10, java.util.concurrent.TimeUnit.SECONDS), "Generated program did not terminate");
        assertEquals(0, program.exitValue());
    }
    @ParameterizedTest @EnumSource(Mode.class)
    void initializerActionsPreserveExternDeclarationsAndDefinitionRelationships(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "extern int shared; int use(){return shared;}", "b.cpp", "int shared=1;"));
        var declaration = named(app, VarDecl.class, "shared", "a.cpp");
        var definition = named(app, VarDecl.class, "shared", "b.cpp");
        definition.setInit(app.getFactory().integerLiteral(7));
        assertFalse(declaration.hasInit());
        assertSame(definition, declaration.getDefinition());
        definition.removeInit(false);
        assertSame(definition, declaration.getDefinition()); // uninitialized global remains a definition
        definition.setInit(app.getFactory().integerLiteral(9));
        assertSame(definition, declaration.getDefinition());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void generatedDefinitionBodyCanBeChangedAfterManagedRename(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int target(){return 1;} int replacement(){return 2;}"));
        var target = function(app, "target", "a.cpp");
        var query = prototype(app, target, app.getTranslationUnits().get(0));
        target.detach();
        query.setName("generated");
        query.setBody(function(app, "replacement", "a.cpp").getBody().orElseThrow().copy());
        assertSame(query, query.getImplementation().orElseThrow());
        query.setBody(target.getBody().orElseThrow().copy());
        assertSame(query, query.getImplementation().orElseThrow());
    }
    private FunctionDecl prototype(App app, FunctionDecl target, TranslationUnit unit) {
        var generated = app.getFactory().functionDecl(target.getDeclName(), target.getType().copy());
        generated.setParameters(target.getParameters().stream()
                .map(p -> app.getFactory().parmVarDecl(p.getDeclName(), p.getType().copy())).toList());
        unit.addChild(generated);
        return generated;
    }
    private VarDecl external(App app, String name, pt.up.fe.specs.clava.ast.type.Type type, TranslationUnit unit) {
        var result = app.getFactory().varDecl(name, type);
        result.set(VarDecl.STORAGE_CLASS, StorageClass.Extern);
        result.set(NamedDecl.LINKAGE, Linkage.External);
        unit.addChild(result);
        return result;
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void generatedFunctionsTrackInsertRemoveAndReplacementWithoutClearCache(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int target(int x){return x;}", "b.cpp", "int other(){return 2;}"));
        var target = function(app, "target", "a.cpp");
        var unit = function(app, "other", "b.cpp").getAncestor(TranslationUnit.class);
        assertSame(target, target.getImplementation().orElseThrow()); // populate index before insertion
        var query = prototype(app, target, unit);
        assertSame(target, query.getImplementation().orElseThrow());
        target.detach();
        assertEquals(Kind.MISSING, app.getFunctionDefinitionResolution(query).kind());
        var replacement = prototype(app, target, unit);
        replacement.setBody(target.getBody().orElseThrow().copy());
        assertSame(replacement, query.getImplementation().orElseThrow());
        replacement.detach();
        assertEquals(Kind.MISSING, app.getFunctionDefinitionResolution(query).kind());
        unit.addChild(target);
        assertSame(target, query.getImplementation().orElseThrow());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void signatureAndRenameHistoryPreservesGeneratedCohort(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int target(int x){return x;}", "b.cpp", "int other(){return 2;}"));
        var target = function(app, "target", "a.cpp");
        var query = prototype(app, target, function(app, "other", "b.cpp").getAncestor(TranslationUnit.class));
        for (int i = 0; i < 12; i++) {
            var selected = i % 2 == 0 ? target : query;
            selected.setDeclName("renamed_" + i);
            selected.getParameters().get(0).setType(app.getFactory().builtinType(i % 2 == 0 ? "long" : "int"));
            selected.setReturnType(app.getFactory().builtinType(i % 2 == 0 ? "long" : "int"));
            assertEquals("renamed_" + i, target.getDeclName());
            assertEquals(target.getDeclName(), query.getDeclName());
            assertEquals(target.getParameters().get(0).getType().getCode(), query.getParameters().get(0).getType().getCode());
            assertSame(target, query.getImplementation().orElseThrow());
            assertFalse(query.hasValue(NamedDecl.USR), "A proof must not manufacture compiler provenance");
            if (i % 3 == 0) app.clearCache();
        }
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void overloadCollisionCanBeDiagnosedAndRepairedWithoutMergingEntities(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int first(int x){return x;} int second(int x){return x+1;}"));
        var first = function(app, "first", "a.cpp");
        var second = function(app, "second", "a.cpp");
        first.setName("second");
        assertEquals(Kind.AMBIGUOUS, app.getFunctionDefinitionResolution(first).kind());
        first.setName("repaired");
        assertEquals("second", second.getDeclName());
        assertSame(first, first.getImplementation().orElseThrow());
        assertSame(second, second.getImplementation().orElseThrow());
        assertFalse(app.isSameFunctionEntity(first, second));
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void bodyReplacementDoesNotTurnPrototypesIntoDefinitions(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int target(); int use(){return target();}",
                "b.cpp", "int target(){return 1;} int replacement(){return 2;}"));
        var declaration = function(app, "target", "a.cpp");
        var definition = function(app, "target", "b.cpp");
        definition.setBody(function(app, "replacement", "b.cpp").getBody().orElseThrow().copy());
        assertFalse(declaration.hasBody());
        assertSame(definition, declaration.getImplementation().orElseThrow());
        assertTrue(definition.getCode().contains("return 2;"));
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void generatedVariablesMatchAndFollowTypeAndNameActions(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int shared=3;", "b.cpp", "int other=4;"));
        var definition = named(app, VarDecl.class, "shared", "a.cpp");
        var query = external(app, "shared", app.getFactory().builtinType("int"),
                named(app, VarDecl.class, "other", "b.cpp").getAncestor(TranslationUnit.class));
        assertSame(definition, query.getDefinition());
        query.setType(app.getFactory().builtinType("long"));
        query.setDeclName("renamed");
        assertEquals("renamed", definition.getDeclName());
        assertEquals("long", definition.getType().getCode());
        assertSame(definition, query.getDefinition());
        assertFalse(query.hasValue(NamedDecl.USR));
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void variableDefinitionQueriesExposeConflictsEvenWhenQueriedOnDefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int shared=3;", "b.cpp", "int shared=4;"));
        var definition = named(app, VarDecl.class, "shared", "a.cpp");
        assertEquals(Kind.AMBIGUOUS, app.getGlobalVarDefinitionResolution(definition).kind());
        assertNull(definition.getDefinition());
        var query = external(app, "shared", app.getFactory().builtinType("int"), app.getTranslationUnits().get(0));
        assertEquals(Kind.AMBIGUOUS, app.getGlobalVarDefinitionResolution(query).kind());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void generatedVariableTypeConflictsFailRatherThanSelectingByName(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int shared=3;"));
        var definition = named(app, VarDecl.class, "shared", "a.cpp");
        var query = external(app, "shared", app.getFactory().builtinType("double"), app.getTranslationUnits().get(0));
        assertEquals(Kind.INCOMPATIBLE, app.getGlobalVarDefinitionResolution(query).kind());
        assertEquals(List.of(definition), app.getGlobalVarDefinitionResolution(query).candidates());
        assertNull(query.getDefinition());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void staticGeneratedObjectsCannotReachOtherTranslationUnits(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "static int shared=3;", "b.cpp", "static int shared=4;"));
        for (var unit : app.getTranslationUnits()) {
            var definition = named(app, VarDecl.class, "shared", unit.getFilename());
            var query = app.getFactory().varDecl("shared", app.getFactory().builtinType("int"));
            query.set(VarDecl.STORAGE_CLASS, StorageClass.Static);
            unit.addChild(query);
            // Two definitions in this TU must conflict, rather than selecting the other TU's object.
            assertEquals(Kind.AMBIGUOUS, app.getGlobalVarDefinitionResolution(query).kind());
            query.detach();
            assertSame(definition, definition.getDefinition());
        }
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void generatedForwardRecordsAndTheirFunctionParametersBindNominally(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "struct R{int value;}; int target(R* r){return r->value;}",
                "b.cpp", "int other(){return 2;}"));
        var record = named(app, RecordDecl.class, "R", "a.cpp");
        var forward = app.getFactory().recordDecl("R", TagKind.STRUCT, List.of());
        forward.set(TagDecl.IS_COMPLETE_DEFINITION, false);
        var unit = function(app, "other", "b.cpp").getAncestor(TranslationUnit.class);
        unit.addChild(forward);
        assertSame(record, app.getTagDefinitionResolution(forward).unique().orElseThrow());
        var target = function(app, "target", "a.cpp");
        var query = app.getFactory().functionDecl("target", app.getFactory().functionProtoType(app.getFactory().builtinType("int"),
                app.getFactory().pointerType(app.getFactory().recordType(forward))));
        query.setParameters(List.of(app.getFactory().parmVarDecl("r", query.getFunctionType().getParamTypes().get(0))));
        unit.addChild(query);
        assertSame(target, query.getImplementation().orElseThrow());
        forward.setDeclName("Renamed");
        assertEquals("Renamed", record.getDeclName());
        assertSame(target, query.getImplementation().orElseThrow());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void unknownTypeEvidenceNeverBecomesAnIdentityEvenForTheSameObject(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int other(){return 1;}"));
        var unknown = app.getFactory().literalType("int");
        var first = external(app, "shared", unknown, app.getTranslationUnits().get(0));
        var second = external(app, "shared", unknown, app.getTranslationUnits().get(0));
        assertEquals(Kind.UNSUPPORTED, app.getGlobalVarDefinitionResolution(first).kind());
        assertEquals(Kind.UNSUPPORTED, app.getGlobalVarDefinitionResolution(second).kind());
        assertThrows(IllegalStateException.class, () -> first.setDeclName("changed"));
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void parsedUnknownTypesStillUseCompilerEvidence(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "typedef int V __attribute__((vector_size(16))); extern V value;",
                "b.cpp", "typedef int V __attribute__((vector_size(16))); V value;"));
        var query = named(app, VarDecl.class, "value", "a.cpp");
        assertSame(named(app, VarDecl.class, "value", "b.cpp"), query.getDefinition());
        var generated = external(app, "value", query.getType(), app.getTranslationUnits().get(0));
        assertEquals(Kind.UNSUPPORTED, app.getGlobalVarDefinitionResolution(generated).kind());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void rawMutationIsNotBlessedByAnotherManagedActionOrCacheReset(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int first(int x){return x;} int second(){return 2;}"));
        var first = function(app, "first", "a.cpp");
        first.set(NamedDecl.DECL_NAME, "raw_change");
        function(app, "second", "a.cpp").setName("safe_change");
        app.clearCache();
        assertEquals(Kind.STALE, app.getFunctionDefinitionResolution(first).kind());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void globalManagerUsesLiveTypesAndRecreatesRemovedDefinitions(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int other(){return 1;}", "b.cpp", "int another(){return 2;}"));
        var units = app.getTranslationUnits();
        var factory = app.getFactory();
        var definition = app.getGlobalManager().addGlobal(units.get(0), "shared", factory.builtinType("int"), factory.integerLiteral(1));
        var declaration = app.getGlobalManager().addGlobal(units.get(1), "shared", factory.builtinType("int"), factory.integerLiteral(2));
        assertTrue(definition.hasInit());
        assertFalse(declaration.hasInit());
        assertSame(definition, declaration.getDefinition());
        int children = units.get(1).getNumChildren();
        assertThrows(IllegalStateException.class, () -> app.getGlobalManager().addGlobal(units.get(1), "shared", factory.builtinType("double"), factory.integerLiteral(3)));
        assertEquals(children, units.get(1).getNumChildren(), "Failed insertion must roll back");
        definition.detach();
        var replacement = app.getGlobalManager().addGlobal(units.get(0), "shared", factory.builtinType("int"), factory.integerLiteral(4));
        assertTrue(replacement.hasInit());
        assertSame(replacement, declaration.getDefinition());
    }
}
