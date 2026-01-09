package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.Arrays;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.CUDAKernelCallExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACudaKernelCall;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.util.SpecsCollections;

public class CXXCudaKernelCall extends ACudaKernelCall {

    private final CUDAKernelCallExpr kernelCall;

    public CXXCudaKernelCall(CUDAKernelCallExpr kernelCall, CxxWeaver weaver) {
        super(new CxxCall(kernelCall, weaver), weaver);

        this.kernelCall = kernelCall;
    }

    @Override
    public ClavaNode getNode() {
        return kernelCall;
    }

    @Override
    public AExpression[] getConfigArrayImpl() {
        return CxxJoinpoints.create(kernelCall.getConfiguration(), getWeaverEngine(), AExpression.class);
    }

    @Override
    public void setConfigImpl(AExpression[] args) {
        kernelCall.setConfiguration(SpecsCollections.toList(args, jp -> (Expr) jp.getNode()));
    }

    @Override
    public void setConfigFromStringsImpl(String[] args) {
        var exprArray = Arrays.stream(args)
                .map(arg -> AstFactory.exprLiteral(getWeaverEngine(), arg))
                .toArray(size -> new AExpression[size]);

        setConfigImpl(exprArray);
    }

}
