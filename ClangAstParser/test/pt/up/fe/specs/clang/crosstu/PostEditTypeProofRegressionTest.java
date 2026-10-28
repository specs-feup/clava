/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.type.*;
import pt.up.fe.specs.clava.ast.type.enums.*;
import pt.up.fe.specs.clava.utils.*;
import pt.up.fe.specs.clava.utils.DefinitionResolution.Kind;

/** Function parameter equivalence is checked against g++'s std::is_same, independently of Clava. */
class PostEditTypeProofRegressionTest extends CrossTuTestSupport {
    static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of("int $", "const int $", true),
            Arguments.of("volatile int $", "int $", true),
            Arguments.of("I $", "int $", true),
            Arguments.of("signed int $", "int $", true),
            Arguments.of("unsigned int $", "int $", false),
            Arguments.of("long $", "long long $", false),
            Arguments.of("int* $", "int* const $", true),
            Arguments.of("const int* $", "int* $", false),
            Arguments.of("volatile int* $", "int* $", false),
            Arguments.of("int* const* $", "int** $", false),
            Arguments.of("int $[3]", "int* $", true),
            Arguments.of("int $[3]", "int $[9]", true),
            Arguments.of("int $[3][4]", "int (*$)[4]", true),
            Arguments.of("int $[3][4]", "int $[3][5]", false),
            Arguments.of("const int $[3]", "int* $", false),
            Arguments.of("int (&$)[3]", "int (&$)[4]", false),
            Arguments.of("int &$", "int &&$", false),
            Arguments.of("const int &$", "int &$", false),
            Arguments.of("int $()", "int (*$)()", true),
            Arguments.of("int (*$)(int)", "int (*$)(long)", false),
            Arguments.of("int (*$)(int)", "int (*$)(int, ...)", false),
            Arguments.of("int (*$)() noexcept", "int (*$)()", false),
            Arguments.of("R* $", "S* $", false),
            Arguments.of("R* $", "struct R* $", true),
            Arguments.of("int R::* $", "int S::* $", false),
            Arguments.of("int (R::*$)() const", "int (R::*$)()", false),
            Arguments.of("int (R::*$)() &", "int (R::*$)() &&", false),
            Arguments.of("E $", "F $", false));
    }

    @ParameterizedTest(name="{0} / {1}: {2}") @MethodSource("parameters")
    void generatedFunctionSelectionAgreesWithIndependentCompiler(String left, String right, boolean equal) throws Exception {
        String source = "#include <type_traits>\nstruct R{}; struct S{}; enum E{e}; enum F{f}; using I=int;\n"
                + "int left(" + left.replace("$", "arg") + "){return 1;}\n"
                + "int target(" + right.replace("$", "arg") + "){return 2;}\n"
                + "static_assert(std::is_same_v<decltype(left),decltype(target)> == " + equal + ");\n";
        Path oracle = temporary.resolve("oracle.cpp");
        Files.writeString(oracle, source);
        var compiler = new ProcessBuilder("g++", "-std=c++20", "-fsyntax-only", oracle.toString()).redirectErrorStream(true).start();
        var diagnostics = new String(compiler.getInputStream().readAllBytes());
        assertEquals(0, compiler.waitFor(), diagnostics);
        var app = parse(Mode.FORWARD, sources("a.cpp", source));
        var original = function(app, "left", "a.cpp");
        var target = function(app, "target", "a.cpp");
        var query = app.getFactory().functionDecl("target", original.getType().copy());
        query.setParameters(original.getParameters().stream()
                .map(p -> app.getFactory().parmVarDecl("arg", p.getType().copy())).toList());
        app.getTranslationUnits().get(0).addChild(query);
        assertEquals(equal ? Kind.UNIQUE : Kind.MISSING, app.getFunctionDefinitionResolution(query).kind());
        if (equal) assertSame(target, query.getImplementation().orElseThrow());
    }

    @Test void arrayBoundsUseFullPrecisionAndIncompleteCompatibilityIsExplicit() throws Exception {
        var app = parse(Mode.FORWARD, sources("a.cpp", "int other(){return 1;}"));
        var f = app.getFactory();
        var first = f.constantArrayType(f.builtinType("int"), BigInteger.ONE.shiftLeft(64).add(BigInteger.valueOf(3)));
        var second = f.constantArrayType(f.builtinType("int"), BigInteger.valueOf(3));
        assertEquals(TypeSemantics.Verdict.INCOMPATIBLE, new TypeSemantics().compare(first, second, true));
        var incomplete = f.incompleteArrayType(f.builtinType("int"));
        assertEquals(TypeSemantics.Verdict.INCOMPATIBLE, new TypeSemantics().compare(incomplete, second, false));
        assertEquals(TypeSemantics.Verdict.COMPATIBLE, new TypeSemantics().compare(incomplete, second, true));
        assertEquals(TypeSemantics.Verdict.INCOMPATIBLE, new TypeSemantics().compare(incomplete, f.constantArrayType(f.builtinType("long"), 3), true));
    }

    @Test void addressSpaceNumbersAreNotErasedByParameterAdjustment() throws Exception {
        var app = parse(Mode.FORWARD, sources("a.cpp", "int other(){return 1;}"));
        var f = app.getFactory();
        var first = f.qualType(f.pointerType(f.builtinType("int")), C99Qualifier.CONST);
        var second = f.qualType(f.pointerType(f.builtinType("int")), C99Qualifier.CONST);
        first.set(QualType.ADDRESS_SPACE, 1L);
        second.set(QualType.ADDRESS_SPACE, 2L);
        assertNotEquals(new TypeSemantics().parameter(first).shape(), new TypeSemantics().parameter(second).shape());
    }

    @Test void generatedInPlaceQualifierChangesAreReprovedOnEveryLookup() throws Exception {
        var app = parse(Mode.FORWARD, sources("a.cpp", "extern int value; int value=1;"));
        var parsed = app.getDescendants(VarDecl.class).stream().filter(VarDecl::hasInit).findFirst().orElseThrow();
        var qualified = app.getFactory().qualType(app.getFactory().builtinType("int"), C99Qualifier.CONST);
        qualified.set(QualType.C99_QUALIFIERS, new ArrayList<>());
        var query = app.getFactory().varDecl("value", qualified);
        query.set(VarDecl.STORAGE_CLASS, pt.up.fe.specs.clava.ast.decl.enums.StorageClass.Extern);
        app.getTranslationUnits().get(0).addChild(query);
        assertSame(parsed, app.getGlobalVarDefinition(query).orElseThrow());
        qualified.get(QualType.C99_QUALIFIERS).add(C99Qualifier.CONST);
        assertEquals(Kind.INCOMPATIBLE, app.getGlobalVarDefinitionResolution(query).kind());
        qualified.get(QualType.C99_QUALIFIERS).clear();
        assertSame(parsed, app.getGlobalVarDefinition(query).orElseThrow());
    }
}
