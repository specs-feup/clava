package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ast.stmt.SwitchCase;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ASwitchCase;

public class CxxSwitchCase<Self extends CxxSwitchCase<Self>> extends ASwitchCase<Self> {

    public CxxSwitchCase(SwitchCase switchCase, CxxWeaver weaver) {
        super(switchCase, weaver);
    }


    @Override
    public SwitchCase getNodeImpl() {
        return (SwitchCase) super.getNodeImpl();
    }
}
