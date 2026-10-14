package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.Arrays;

import pt.up.fe.specs.clava.ast.expr.CUDAKernelCallExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACudaKernelCall;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.util.SpecsCollections;

public class CxxCudaKernelCall<Self extends CxxCudaKernelCall<Self>> extends ACudaKernelCall<Self> {

    public CxxCudaKernelCall(CUDAKernelCallExpr kernelCall, CxxWeaver weaver) {
        super(kernelCall, weaver);
    }

    @Override
    public CUDAKernelCallExpr getNodeImpl() {
        return (CUDAKernelCallExpr) super.getNodeImpl();
    }

    @Override
    public AExpression<?>[] getConfigImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getConfiguration(), getWeaverEngine(), AExpression.class);
    }

    @Override
    public void setConfigImpl(AExpression<?>[] args) {
        this.getNodeImpl().setConfiguration(SpecsCollections.toList(args, jp -> (Expr) jp.getNodeImpl()));
    }

    @Override
    public void setConfigFromStringsImpl(String[] args) {
        var exprArray = Arrays.stream(args)
                .map(arg -> AstFactory.exprLiteral(getWeaverEngine(), arg))
                .toArray(AExpression[]::new);

        setConfigImpl(exprArray);
    }

}
