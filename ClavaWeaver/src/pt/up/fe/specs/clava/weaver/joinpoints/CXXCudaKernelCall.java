package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.Arrays;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.CUDAKernelCallExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACudaKernelCall;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.util.SpecsCollections;

public class CXXCudaKernelCall extends ACudaKernelCall {

    private final CUDAKernelCallExpr kernelCall;

    public CXXCudaKernelCall(CUDAKernelCallExpr kernelCall) {
        super(new CxxCall(kernelCall));

        this.kernelCall = kernelCall;
    }

    @Override
    public ClavaNode getNode() {
        return kernelCall;
    }

    @Override
    public AExpression[] getConfigArrayImpl() {
        return CxxJoinpoints.create(kernelCall.getConfiguration(), AExpression.class);
    }

    @Override
    public void defConfigImpl(AExpression[] args) {
        kernelCall.setConfiguration(SpecsCollections.toList(args, jp -> (Expr) jp.getNode()));
    }

    @Override
    public void setConfigImpl(AExpression[] args) {
        defConfigImpl(args);
    }

    @Override
    public void setConfigFromStringsImpl(String[] args) {
        var exprArray = Arrays.stream(args)
                .map(AstFactory::exprLiteral)
                .toArray(size -> new AExpression[size]);

        setConfigImpl(exprArray);
    }

}
