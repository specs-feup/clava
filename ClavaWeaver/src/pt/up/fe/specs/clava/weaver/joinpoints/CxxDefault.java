package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.stmt.DefaultStmt;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADefault;

public class CxxDefault extends ADefault {

    private final DefaultStmt defaultStmt;

    public CxxDefault(DefaultStmt defaultStmt) {
        super(new CxxSwitchCase(defaultStmt));
        this.defaultStmt = defaultStmt;
    }
    
    @Override
    public ClavaNode getNode() {
        return defaultStmt;
    }
}
