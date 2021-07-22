package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.util.SpecsCollections;

public class CUDAKernelCallExpr extends CallExpr {

    public CUDAKernelCallExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public List<Expr> getConfiguration() {
        // Second child should be a CallExpr
        var call = getChild(CallExpr.class, 1);

        return call.getArgs().stream()
                // Remove default arguments
                .filter(arg -> !(arg instanceof CXXDefaultArgExpr))
                .collect(Collectors.toList());
    }

    public List<Expr> getArgs() {

        if (getNumChildren() <= 2) {
            return Collections.emptyList();
        }

        return SpecsCollections.cast(getChildren().subList(2, getNumChildren()), Expr.class);
    }

    @Override
    public String getCode() {
        // add<<<10,1024>>>(a);

        var code = new StringBuilder();

        code.append(getCalleeCode());
        code.append("<<<");
        code.append(getConfiguration().stream().map(Expr::getCode).collect(Collectors.joining(", ")));
        code.append(">>>");
        code.append(getArgsCode());
        code.append("");

        return code.toString();
    }

}
