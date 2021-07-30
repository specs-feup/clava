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

        return getInternalCall().getArgs().stream()
                // Remove default arguments
                .filter(arg -> !(arg instanceof CXXDefaultArgExpr))
                .collect(Collectors.toList());
    }

    private CallExpr getInternalCall() {
        // Second child should be a CallExpr
        return getChild(CallExpr.class, 1);
    }

    public void setConfiguration(List<Expr> config) {
        getInternalCall().setArguments(config);
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
