package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceLocation;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;

/** A C++20 comparison retaining both source syntax and Clang's rewritten form. */
public class CXXRewrittenBinaryOperator extends Expr {

    public static final DataKey<BinaryOperatorKind> OP =
            KeyFactory.enumeration("op", BinaryOperatorKind.class);

    public static final DataKey<Expr> LHS = KeyFactory.object("lhs", Expr.class);

    public static final DataKey<Expr> RHS = KeyFactory.object("rhs", Expr.class);

    public static final DataKey<Expr> SEMANTIC_FORM =
            KeyFactory.object("semanticForm", Expr.class);

    public static final DataKey<Expr> INNER_BIN_OP =
            KeyFactory.object("innerBinOp", Expr.class);

    public static final DataKey<Boolean> IS_REVERSED = KeyFactory.bool("isReversed");

    public static final DataKey<SourceLocation> OPERATOR_LOCATION =
            KeyFactory.object("operatorLocation", SourceLocation.class);

    public CXXRewrittenBinaryOperator(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        return get(LHS).getCode() + " " + get(OP).getOpString() + " " + get(RHS).getCode();
    }
}
