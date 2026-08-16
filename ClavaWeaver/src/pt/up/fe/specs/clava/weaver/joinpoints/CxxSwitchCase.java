package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.stmt.SwitchCase;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ASwitchCase;

public class CxxSwitchCase extends ASwitchCase {

    private final SwitchCase switchCase;

    public CxxSwitchCase(SwitchCase switchCase, CxxWeaver weaver) {
        super(new CxxStatement(switchCase, weaver), weaver);
        this.switchCase = switchCase;
    }


    @Override
    public ClavaNode getNode() {
        return switchCase;
    }
}
