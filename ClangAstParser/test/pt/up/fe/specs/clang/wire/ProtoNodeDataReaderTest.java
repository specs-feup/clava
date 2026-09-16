/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import java.util.List;

import org.junit.jupiter.api.Test;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.ArrayType;
import pt.up.fe.specs.clava.context.ClavaContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtoNodeDataReaderTest {

    @Test
    void mapsGeneratedNodeIntoTheExistingDataStore() {
        var data = new ClangAstData();
        data.set(ClangAstData.CONTEXT, new ClavaContext());
        data.set(ClangAstData.CLAVA_NODES, new ClavaNodes(
                data.get(ClangAstData.CONTEXT).get(ClavaContext.FACTORY)));
        var wire = Node.newBuilder()
                .setId(1)
                .setClassName("BuiltinType")
                .setBuiltinTypeData(BuiltinTypeData.newBuilder()
                        .setBase(TypeData.newBuilder()
                                .setTypeAsString("int")
                                .setTypeDependency(pt.up.fe.specs.clang.wire.TypeDependency.TYPEDEPENDENCY_NONE)
                                .setIsVariablyModified(false)
                                .setContainsUnexpandedParameterPack(false)
                                .setIsFromAst(false)
                                .setUnqualifiedDesugaredType(-1))
                        .setKind(BuiltinKind.BUILTINKIND_INT)
                        .setKindLiteral("int"))
                .build();

        var result = ProtoNodeDataReader.read(wire, data, value -> "@" + value + "_test",
                new ProtoAstReader.Files("test"));

        assertEquals("@1_test", result.get(pt.up.fe.specs.clava.ClavaNode.ID));
        assertEquals("int", result.get(pt.up.fe.specs.clava.ast.type.Type.TYPE_AS_STRING));
        assertEquals(pt.up.fe.specs.clava.ast.type.enums.BuiltinKind.Int, result.get(BuiltinType.KIND));
    }

    @Test
    void rejectsAnAbsentOptionalScalarInsteadOfUsingItsDefault() {
        var data = new ClangAstData();
        data.set(ClangAstData.CONTEXT, new ClavaContext());
        data.set(ClangAstData.CLAVA_NODES, new ClavaNodes(
                data.get(ClangAstData.CONTEXT).get(ClavaContext.FACTORY)));
        var wire = Node.newBuilder()
                .setId(1)
                .setClassName("BuiltinType")
                .setBuiltinTypeData(BuiltinTypeData.newBuilder()
                        .setBase(TypeData.newBuilder()
                                .setTypeAsString("int")
                                .setTypeDependency(TypeDependency.TYPEDEPENDENCY_NONE)
                                .setIsVariablyModified(false)
                                .setContainsUnexpandedParameterPack(false)
                                .setIsFromAst(false)
                                .setUnqualifiedDesugaredType(-1))
                        .setKind(BuiltinKind.BUILTINKIND_INT)
                        .clearKindLiteral())
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> ProtoNodeDataReader.read(wire, data, value -> "@" + value + "_test",
                        new ProtoAstReader.Files("test")));
    }

    @Test
    void mapsRepeatedEnumsThroughTheGeneratedBinding() {
        var data = new ClangAstData();
        data.set(ClangAstData.CONTEXT, new ClavaContext());
        data.set(ClangAstData.CLAVA_NODES, new ClavaNodes(
                data.get(ClangAstData.CONTEXT).get(ClavaContext.FACTORY)));
        var wire = Node.newBuilder()
                .setId(2)
                .setClassName("ArrayType")
                .setArrayTypeData(ArrayTypeData.newBuilder()
                        .setBase(TypeData.newBuilder()
                                .setTypeAsString("int[1]")
                                .setTypeDependency(TypeDependency.TYPEDEPENDENCY_NONE)
                                .setIsVariablyModified(false)
                                .setContainsUnexpandedParameterPack(false)
                                .setIsFromAst(false)
                                .setUnqualifiedDesugaredType(-1))
                        .setArraySizeModifier(ArraySizeModifier.ARRAYSIZEMODIFIER_NORMAL)
                        .addIndexTypeQualifiers(C99Qualifier.C99QUALIFIER_CONST)
                        .setElementType(-1))
                .build();

        var result = ProtoNodeDataReader.read(wire, data, value -> "@" + value + "_test",
                new ProtoAstReader.Files("test"));

        assertEquals(List.of(pt.up.fe.specs.clava.ast.type.enums.C99Qualifier.CONST),
                result.get(ArrayType.INDEX_TYPE_QUALIFIERS));
    }
}
