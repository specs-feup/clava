package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ast.stmt.AsmStmt;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AAsmStmt;

public class CxxAsmStmt<Self extends CxxAsmStmt<Self>> extends AAsmStmt<Self> {

    public CxxAsmStmt(AsmStmt asmStmt, CxxWeaver weaver) {
        super(asmStmt, weaver);
    }

    @Override
    public AsmStmt getNodeImpl() {
        return (AsmStmt) super.getNodeImpl();
    }

    @Override
    public String[] getClobbersImpl() {
        return this.getNodeImpl().get(AsmStmt.CLOBBERS).toArray(new String[0]);
    }

    @Override
    public boolean getIsSimpleImpl() {
        return this.getNodeImpl().get(AsmStmt.IS_SIMPLE);
    }

    @Override
    public boolean getIsVolatileImpl() {
        return this.getNodeImpl().get(AsmStmt.IS_VOLATILE);
    }

}
