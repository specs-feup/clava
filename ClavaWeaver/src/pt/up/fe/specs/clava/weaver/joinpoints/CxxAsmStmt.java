package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.stmt.AsmStmt;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AAsmStmt;

public class CxxAsmStmt extends AAsmStmt {

    private final AsmStmt asmStmt;

    /**
     * @param asmStmt
     */
    public CxxAsmStmt(AsmStmt asmStmt) {
        super(new CxxStatement(asmStmt));

        this.asmStmt = asmStmt;
    }

    @Override
    public ClavaNode getNode() {
        return asmStmt;
    }

    @Override
    public String[] getClobbersArrayImpl() {
        return asmStmt.get(AsmStmt.CLOBBERS).toArray(new String[0]);
    }

    @Override
    public Boolean getIsSimpleImpl() {
        return asmStmt.get(AsmStmt.IS_SIMPLE);
    }

    @Override
    public Boolean getIsVolatileImpl() {
        return asmStmt.get(AsmStmt.IS_VOLATILE);
    }


}
