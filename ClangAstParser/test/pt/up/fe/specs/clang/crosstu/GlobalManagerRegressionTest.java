/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.utils.GlobalManager;

class GlobalManagerRegressionTest {
    @Test void addingASharedGlobalEmitsOneInitializerAndExternDeclarations() {
        var factory = new ClavaContext().getFactory();
        var firstTu = factory.translationUnit(new File("first.cpp"), List.of());
        var secondTu = factory.translationUnit(new File("second.cpp"), List.of());
        var manager = new GlobalManager();
        var first = manager.addGlobal(firstTu, "shared", factory.builtinType("int"), factory.integerLiteral(7));
        var second = manager.addGlobal(secondTu, "shared", factory.builtinType("int"), factory.integerLiteral(9));
        assertTrue(first.hasInit());
        assertEquals("7", first.getInit().orElseThrow().getCode());
        assertEquals(StorageClass.None, first.get(VarDecl.STORAGE_CLASS));
        assertFalse(second.hasInit());
        assertEquals(StorageClass.Extern, second.get(VarDecl.STORAGE_CLASS));
        assertSame(firstTu, first.getAncestor(pt.up.fe.specs.clava.ast.extra.TranslationUnit.class));
        assertSame(secondTu, second.getAncestor(pt.up.fe.specs.clava.ast.extra.TranslationUnit.class));
    }

    @Test void distinctNamesEachGetTheirOwnDefinition() {
        var factory = new ClavaContext().getFactory();
        var tu = factory.translationUnit(new File("first.cpp"), List.of());
        var manager = new GlobalManager();
        var first = manager.addGlobal(tu, "first", factory.builtinType("int"), factory.integerLiteral(1));
        var second = manager.addGlobal(tu, "second", factory.builtinType("int"), factory.integerLiteral(2));
        assertTrue(first.hasInit());
        assertTrue(second.hasInit());
        assertEquals(2, tu.getChildren().size());
    }

    @Test void independentlyCreatedApplicationsDoNotShareGlobalInsertionState() {
        var factory = new ClavaContext().getFactory();
        var first = factory.translationUnit(new File("first.cpp"), List.of());
        var second = factory.translationUnit(new File("second.cpp"), List.of());
        var firstApp = factory.app(List.of(first));
        var secondApp = factory.app(List.of(second));
        assertTrue(firstApp.getGlobalManager().addGlobal(first, "shared", factory.builtinType("int"), factory.integerLiteral(1)).hasInit());
        assertTrue(secondApp.getGlobalManager().addGlobal(second, "shared", factory.builtinType("int"), factory.integerLiteral(2)).hasInit());
    }
}
