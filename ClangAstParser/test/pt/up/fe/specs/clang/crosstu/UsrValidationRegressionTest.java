/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.extra.data.Language;
import pt.up.fe.specs.clava.ast.type.*;
import pt.up.fe.specs.clava.ast.type.data.ExceptionSpecification;
import pt.up.fe.specs.clava.ast.type.enums.*;
import pt.up.fe.specs.clava.utils.DefinitionResolution.Kind;

/** Warm lookup caches must still observe nested edits and APIs that bypass node setters. */
class UsrValidationRegressionTest extends CrossTuTestSupport {
    enum Edit { NESTED_TYPE, PARAMETER_LIST, QUALIFIER_LIST, EXCEPTION_DATA, RAW_CHILD_LIST,
        SHARED_TYPE_VIEW, INDEPENDENT_COPY, EQUIVALENT_THEN_DIFFERENT, INVALID_USR, LANGUAGE_DATA, REPLACED_LANGUAGE, TU_BOOKKEEPING }

    @ParameterizedTest @EnumSource(Edit.class)
    void warmLookupRetainsMutationChecks(Edit edit) throws Exception {
        boolean pointer = edit == Edit.QUALIFIER_LIST;
        String parameter = pointer ? "const int*" : "int";
        var app = parse(Mode.FORWARD, sources("use.cpp", "int target(" + parameter + ");",
                "impl.cpp", "int target(" + parameter + " x){return 1;}"));
        var query = function(app, "target", "use.cpp");
        var definition = function(app, "target", "impl.cpp");
        assertEquals(Kind.UNIQUE, app.getFunctionDefinitionResolution(query).kind());
        assertEquals(Kind.UNIQUE, app.getFunctionDefinitionResolution(query).kind());
        var type = (FunctionProtoType) definition.getType();
        var result = (BuiltinType) type.get(FunctionType.RETURN_TYPE);
        switch (edit) {
        case NESTED_TYPE -> result.set(BuiltinType.KIND, BuiltinKind.Double);
        case PARAMETER_LIST -> type.get(FunctionProtoType.PARAMETERS_TYPES).set(0,
                definition.getFactory().builtinType(BuiltinKind.Double));
        case QUALIFIER_LIST -> ((QualType) ((PointerType) type.getParamTypes().get(0)).getPointeeType())
                .get(QualType.C99_QUALIFIERS).clear();
        case EXCEPTION_DATA -> type.get(FunctionProtoType.EXCEPTION_SPECIFICATION)
                .set(ExceptionSpecification.EXCEPTION_SPECIFICATION_TYPE, ExceptionSpecificationType.BasicNoexcept);
        case RAW_CHILD_LIST -> definition.getChildrenMutable().remove(0);
        case SHARED_TYPE_VIEW -> result.newInstance(true, true, BuiltinType.class, List.of())
                .set(BuiltinType.KIND, BuiltinKind.Double);
        case INDEPENDENT_COPY -> ((BuiltinType) result.copy()).set(BuiltinType.KIND, BuiltinKind.Double);
        case EQUIVALENT_THEN_DIFFERENT -> {
            definition.setType((Type) type.copy());
            assertEquals(Kind.UNIQUE, app.getFunctionDefinitionResolution(query).kind());
            ((BuiltinType) ((FunctionType) definition.getType()).get(FunctionType.RETURN_TYPE))
                    .set(BuiltinType.KIND, BuiltinKind.Double);
        }
        case LANGUAGE_DATA -> definition.getAncestor(TranslationUnit.class).get(TranslationUnit.LANGUAGE)
                .set(Language.C_PLUS_PLUS, false);
        case REPLACED_LANGUAGE -> {
            var unit = definition.getAncestor(TranslationUnit.class);
            unit.set(TranslationUnit.LANGUAGE, new Language().set(unit.get(TranslationUnit.LANGUAGE)));
            assertEquals(Kind.UNIQUE, app.getFunctionDefinitionResolution(query).kind());
            unit.get(TranslationUnit.LANGUAGE).set(Language.C_PLUS_PLUS, false);
        }
        case TU_BOOKKEEPING -> definition.getAncestor(TranslationUnit.class).set(TranslationUnit.HAS_PARSING_ERRORS, true);
        case INVALID_USR -> {
            query.set(NamedDecl.USR, "");
            query.set(NamedDecl.USR_GENERATION_FAILED, true);
        }
        }
        var expected = (edit == Edit.INDEPENDENT_COPY || edit == Edit.TU_BOOKKEEPING) ? Kind.UNIQUE
                : edit == Edit.INVALID_USR ? Kind.UNSUPPORTED : Kind.STALE;
        assertEquals(expected, app.getFunctionDefinitionResolution(query).kind());
        app.clearCache();
        assertEquals(expected, app.getFunctionDefinitionResolution(query).kind());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void changedInternalOriginCannotReuseCachedKeys(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "static int target(); int use(){return target();} int target(){return 1;}"));
        var query = app.getDescendants(pt.up.fe.specs.clava.ast.decl.FunctionDecl.class).stream()
                .filter(f -> f.getDeclName().equals("target") && !f.hasBody()).findFirst().orElseThrow();
        assertEquals(Kind.UNIQUE, app.getFunctionDefinitionResolution(query).kind());
        var unit = query.getAncestor(TranslationUnit.class);
        unit.set(TranslationUnit.SOURCE_FILE, temporary.resolve("relocated.cpp").toFile());
        assertEquals(Kind.STALE, app.getFunctionDefinitionResolution(query).kind());
        app.clearCache();
        assertEquals(Kind.STALE, app.getFunctionDefinitionResolution(query).kind());
    }
}
