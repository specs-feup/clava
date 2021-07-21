package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class CUDAKernelCallExpr extends CallExpr {

	public CUDAKernelCallExpr(DataStore data, Collection<? extends ClavaNode> children) {
		super(data, children);
	}

}
