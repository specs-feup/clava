package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ast.stmt.DefaultStmt;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADefault;

public class CxxDefault<Self extends CxxDefault<Self>> extends ADefault<Self> {

    public CxxDefault(DefaultStmt defaultStmt, CxxWeaver weaver) {
        super(defaultStmt, weaver);
    }
    
    @Override
    public DefaultStmt getNodeImpl() {
        return (DefaultStmt) super.getNodeImpl();
    }
}
