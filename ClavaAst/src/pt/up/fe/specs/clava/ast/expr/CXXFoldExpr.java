/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceLocation;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;

/** A C++ fold expression with Clang's operands and pack-expansion details. */
public class CXXFoldExpr extends Expr {

    public static final DataKey<BinaryOperatorKind> OP =
            KeyFactory.enumeration("op", BinaryOperatorKind.class);

    public static final DataKey<Optional<Expr>> LHS = KeyFactory.optional("lhs");

    public static final DataKey<Optional<Expr>> RHS = KeyFactory.optional("rhs");

    public static final DataKey<Optional<Expr>> INIT = KeyFactory.optional("init");

    public static final DataKey<Expr> PATTERN = KeyFactory.object("pattern", Expr.class);

    public static final DataKey<Optional<Expr>> CALLEE = KeyFactory.optional("callee");

    public static final DataKey<Optional<Integer>> NUM_EXPANSIONS = KeyFactory.optional("numExpansions");

    public static final DataKey<Boolean> IS_LEFT_FOLD = KeyFactory.bool("isLeftFold");

    public static final DataKey<Boolean> IS_RIGHT_FOLD = KeyFactory.bool("isRightFold");

    public static final DataKey<Optional<SourceLocation>> L_PAREN_LOC = KeyFactory.optional("lParenLoc");

    public static final DataKey<Optional<SourceLocation>> ELLIPSIS_LOC = KeyFactory.optional("ellipsisLoc");

    public static final DataKey<Optional<SourceLocation>> R_PAREN_LOC = KeyFactory.optional("rParenLoc");

    public CXXFoldExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        String operator = get(OP).getOpString();
        Optional<Expr> init = get(INIT);
        Expr pattern = get(PATTERN);

        String body;
        if (init.isEmpty()) {
            body = get(IS_RIGHT_FOLD)
                    ? pattern.getCode() + " " + operator + " ..."
                    : "... " + operator + " " + pattern.getCode();
        } else if (get(IS_RIGHT_FOLD)) {
            body = pattern.getCode() + " " + operator + " ... " + operator + " " + init.get().getCode();
        } else {
            body = init.get().getCode() + " " + operator + " ... " + operator + " " + pattern.getCode();
        }
        return "(" + body + ")";
    }
}
