package pt.up.fe.specs.clava.ast.attr;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class CUDAGlobalAttr extends Attribute {

	public CUDAGlobalAttr(DataStore data, Collection<? extends ClavaNode> children) {
		super(data, children);
	}

	@Override
	public String getCode() {
		return "__global__";
	}
}
