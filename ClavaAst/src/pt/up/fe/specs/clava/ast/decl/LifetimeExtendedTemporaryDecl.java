package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.enums.StorageDuration;
import pt.up.fe.specs.clava.ast.expr.Expr;

/** Clang's implicit declaration tying a materialized temporary to its lifetime owner. */
public class LifetimeExtendedTemporaryDecl extends Decl {

    public static final DataKey<ValueDecl> EXTENDING_DECL =
            KeyFactory.object("extendingDecl", ValueDecl.class);

    public static final DataKey<Expr> TEMPORARY_EXPR =
            KeyFactory.object("temporaryExpr", Expr.class);

    public static final DataKey<StorageDuration> STORAGE_DURATION =
            KeyFactory.enumeration("storageDuration", StorageDuration.class);

    public static final DataKey<Integer> MANGLING_NUMBER =
            KeyFactory.integer("manglingNumber");

    public LifetimeExtendedTemporaryDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        // This declaration is an AST link, not a separately written declaration.
        return "";
    }
}
