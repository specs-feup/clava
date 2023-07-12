/**
 * Copyright 2020 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.stmt.SwitchCase;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACase;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;

public class CxxCase extends ACase {

    private final SwitchCase caseStmt;

    public CxxCase(SwitchCase caseStmt) {
        super(new CxxStatement(caseStmt));
        this.caseStmt = caseStmt;
    }

    @Override
    public ClavaNode getNode() {
        return caseStmt;
    }

    @Override
    public Boolean getIsDefaultImpl() {
        return caseStmt.isDefaultCase();
    }

    @Override
    public Boolean getIsEmptyImpl() {
        return caseStmt.isEmptyCase();
    }

    @Override
    public AStatement getNextInstructionImpl() {
        var nextInst = caseStmt.nextExecutedInstruction();
        if (nextInst == null) {
            return null;
        }

        return CxxJoinpoints.create(nextInst, AStatement.class);
    }

    @Override
    public AStatement[] getInstructionsArrayImpl() {
        return CxxJoinpoints.create(caseStmt.getInstructions(), AStatement.class);
    }

    @Override
    public ACase getNextCaseImpl() {
        return CxxJoinpoints.create(caseStmt.nextCase(), ACase.class);
    }

    @Override
    public AExpression[] getValuesArrayImpl() {
        return CxxJoinpoints.create(caseStmt.getValues(), AExpression.class);
    }

}
