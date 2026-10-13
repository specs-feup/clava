/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinitions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.PointerType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.data.NodeDataStore;

class GeneratedNodeDataTest {
    private final ClavaContext context = new ClavaContext();

    @Test
    void concreteBehaviorClassesExtendAbstractDataBases() throws Exception {
        for (var payload : WireSchema.get().unionMembers(WireSchema.get().table("Node").field("payload"))) {
            var node = NodeBindings.of(payload).payloadClass;
            boolean declaresData = java.util.Arrays.stream(node.getFields())
                    .anyMatch(field -> field.getDeclaringClass().getSimpleName().equals(node.getSimpleName() + "Base"));
            if (declaresData) {
                assertEquals(node.getSimpleName() + "Base", node.getSuperclass().getSimpleName());
                assertTrue(Modifier.isAbstract(node.getSuperclass().getModifiers()));
                var constructor = node.getSuperclass().getDeclaredConstructor(DataStore.class, java.util.Collection.class);
                constructor.setAccessible(true);
                assertThrows(InstantiationException.class, () -> constructor.newInstance(store(node), List.of()));
            }
        }
        assertInstanceOf(BuiltinType.class, context.getFactory().builtinType("int"));
        assertInstanceOf(IntegerLiteral.class, context.getFactory().integerLiteral(42));
    }

    @Test
    void generatedAndGenericAccessUseOneValueIncludingRemovalAndDefaults() {
        var node = context.getFactory().builtinType(BuiltinKind.Int);
        node.setWireKindLiteral("custom");
        assertEquals("custom", node.get(BuiltinType.KIND_LITERAL));
        node.set(BuiltinType.KIND_LITERAL, "generic");
        assertEquals("generic", node.getWireKindLiteral());
        node.setWireKindLiteral(null);
        assertFalse(node.hasValue(BuiltinType.KIND_LITERAL));
        assertEquals("", node.getWireKindLiteral());
        assertTrue(node.hasValue(BuiltinType.KIND_LITERAL));
        // This key intentionally has a null default, rather than an Optional wrapper.
        var literal = context.getFactory().integerLiteral(1);
        assertNull(literal.get(Expr.IMPLICIT_CAST));
        assertTrue(literal.getImplicitCast().isEmpty());
    }

    @Test
    void generatedFieldsNamedValueAndIndexAreNotShadowedByAdapterArguments() {
        var literal = context.getFactory().integerLiteral(42);
        assertEquals(java.math.BigInteger.valueOf(42), literal.getWireValue());
        literal.set(IntegerLiteral.VALUE, java.math.BigInteger.valueOf(17));
        assertEquals(java.math.BigInteger.valueOf(17), literal.getWireValue());
        var parameter = new pt.up.fe.specs.clava.ast.type.TemplateTypeParmType(
                store(pt.up.fe.specs.clava.ast.type.TemplateTypeParmType.class), List.of());
        parameter.setWireIndex(3);
        assertEquals(3, parameter.getWireIndex());
        assertEquals(3, parameter.get(pt.up.fe.specs.clava.ast.type.TemplateTypeParmType.INDEX));
    }

    @Test
    void typedValueConstructorCreatesTheBehaviorSubclass() {
        var data = store(BuiltinType.class);
        var values = new pt.up.fe.specs.clava.ast.type.BuiltinTypeBase.Values(BuiltinKind.Int, "typed");
        var node = new BuiltinType(data, List.of(), values);
        assertEquals(BuiltinKind.Int, node.getWireKind());
        assertEquals("typed", node.getWireKindLiteral());
        node.setWireValues(new pt.up.fe.specs.clava.ast.type.BuiltinTypeBase.Values(BuiltinKind.Float, "float"));
        assertEquals(BuiltinKind.Float, node.get(BuiltinType.KIND));
    }

    @Test
    void deferredParserWritesReachFieldsAfterConstruction() {
        var data = NodeDataStore.adapt(store(PointerType.class));
        var node = new PointerType(data, List.of());
        var target = context.getFactory().builtinType("int");
        // The parser queues writes to this store before it constructs the node.
        Runnable deferred = () -> data.set(PointerType.POINTEE_TYPE, target);
        deferred.run();
        assertSame(target, node.getWirePointeeType());
        assertSame(target, data.get(PointerType.POINTEE_TYPE));
        assertFalse(node.getTry(Type.UNQUALIFIED_DESUGARED_TYPE).isPresent());
        assertEquals(Optional.empty(), node.getWireUnqualifiedDesugaredType());
    }

    @Test
    void copiesOwnTheirFieldsAndMutableDefaultsButExplicitViewsShareThem() {
        var original = context.getFactory().builtinType("int");
        original.get(ClavaNode.INLINE_COMMENTS); // Initialize a list with its own copy function.
        var copy = (BuiltinType) original.copy();
        assertNotEquals(original.getId(), copy.getId());
        assertNotSame(original.get(ClavaNode.INLINE_COMMENTS), copy.get(ClavaNode.INLINE_COMMENTS));
        copy.setWireKindLiteral("copy");
        assertEquals("int", original.getWireKindLiteral());
        assertEquals("copy", copy.getWireKindLiteral());
        var shared = original.newInstance(true, true, BuiltinType.class, new ArrayList<>());
        shared.setWireKindLiteral("shared");
        assertEquals("shared", original.getWireKindLiteral());
        var pointer = context.getFactory().pointerType(original);
        var deep = (PointerType) pointer.deepCopy();
        assertNotSame(pointer.getWirePointeeType(), deep.getWirePointeeType());
        assertEquals(pointer.getCode(), deep.getCode());
    }

    @Test
    void viewsOfAnotherBehaviorClassKeepExistingTypedStateReferences() {
        var original = context.getFactory().builtinType("int");
        var sameClassView = original.newInstance(true, true, BuiltinType.class, new ArrayList<>());
        var pointerView = original.newInstance(true, true, PointerType.class, new ArrayList<>());
        pointerView.setWireTypeAsString("changed");
        assertEquals("changed", original.getWireTypeAsString());
        assertEquals("changed", sameClassView.getWireTypeAsString());
        sameClassView.setWireKindLiteral("updated");
        assertEquals("updated", original.getWireKindLiteral());
    }

    private DataStore store(Class<?> nodeClass) {
        return DataStore.newInstance(StoreDefinitions.fromInterface(nodeClass), true)
                .set(ClavaNode.CONTEXT, context).set(ClavaNode.ID, "test");
    }
}
