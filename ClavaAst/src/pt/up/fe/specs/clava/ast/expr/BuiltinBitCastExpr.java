/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/** Represents a C++ builtin bit-cast expression. */
public class BuiltinBitCastExpr extends ExplicitCastExpr {

    public BuiltinBitCastExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        return "__builtin_bit_cast(" + get(TYPE_AS_WRITTEN).getCode(this) + ", " + getSubExpr().getCode() + ")";
    }
}
