package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.stmt.SwitchCase;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ASwitchCase;

public class CxxSwitchCase extends ASwitchCase {

    private final SwitchCase switchCase;

    public CxxSwitchCase(SwitchCase switchCase) {
        super(new CxxStatement(switchCase));
        this.switchCase = switchCase;
    }


    @Override
    public ClavaNode getNode() {
        return switchCase;
    }
}
