package pt.up.fe.specs.clava.ast.attr;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class AlwaysInlineAttr extends Attribute {

    public AlwaysInlineAttr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

}
