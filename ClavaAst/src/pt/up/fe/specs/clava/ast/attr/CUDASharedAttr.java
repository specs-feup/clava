package pt.up.fe.specs.clava.ast.attr;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class CUDASharedAttr extends Attribute {

    public CUDASharedAttr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        return "__shared__";
    }
}
