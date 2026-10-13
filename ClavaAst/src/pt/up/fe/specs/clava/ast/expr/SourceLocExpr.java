/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.language.SourceLocIdentKind;

/** Represents one of Clang's source-location builtin expressions. */
public class SourceLocExpr extends Expr {

    public static final DataKey<SourceLocIdentKind> IDENT_KIND =
            KeyFactory.enumeration("identKind", SourceLocIdentKind.class);

    /** The Clang declaration context, empty for a null or translation-unit context. */
    public static final DataKey<Optional<Decl>> PARENT_CONTEXT = KeyFactory.optional("parentContext");

    public SourceLocExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public SourceLocIdentKind getIdentKind() {
        return get(IDENT_KIND);
    }

    public Optional<Decl> getParentContext() {
        return get(PARENT_CONTEXT);
    }

    @Override
    public String getCode() {
        return getIdentKind().getString() + "()";
    }
}
