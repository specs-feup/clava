/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;
import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.context.ClavaFactory;

abstract class TUnitProcessorTestSupport {
    final ClavaFactory factory = new ClavaContext().getFactory();

    public static class Holder extends Decl {
        public static final DataKey<ClavaNode> DIRECT = KeyFactory.object("direct", ClavaNode.class);
        public static final DataKey<Optional<ClavaNode>> OPTIONAL = KeyFactory.optional("optional");
        public static final DataKey<List<ClavaNode>> NODES = KeyFactory.list("nodes", ClavaNode.class);
        public static final DataKey<List<String>> TEXT = KeyFactory.list("text", String.class);
        public Holder(DataStore data, java.util.Collection<? extends ClavaNode> children) { super(data, children); }
    }

    Holder holder() { return new Holder(factory.newDataStore(Holder.class), List.of()); }

    VarDecl variable(String name, String path, int line) {
        var variable = factory.varDecl(name, factory.builtinType("int"));
        variable.set(ClavaNode.LOCATION, new SourceRange(path, line, 1, line, 8));
        return variable;
    }

    ClangAstData data(String file, List<? extends Decl> roots, ClavaNode... registered) {
        var tu = factory.translationUnit(new File(file), new ArrayList<>(roots));
        var registry = new ClavaNodes(factory);
        for (var node : registered) { registry.getNodes().put(node.getId(), node); }
        var data = new ClangAstData();
        data.set(ClangAstData.TRANSLATION_UNIT, tu);
        data.set(ClangAstData.CLAVA_NODES, registry);
        return data;
    }

}
