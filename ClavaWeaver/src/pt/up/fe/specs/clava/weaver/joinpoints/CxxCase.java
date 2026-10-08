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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ast.stmt.SwitchCase;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACase;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;

public class CxxCase<Self extends CxxCase<Self>> extends ACase<Self> {

    public CxxCase(SwitchCase caseStmt, CxxWeaver weaver) {
        super(caseStmt, weaver);
    }

    @Override
    public SwitchCase getNodeImpl() {
        return (SwitchCase) super.getNodeImpl();
    }

    @Override
    public boolean getIsDefaultImpl() {
        return this.getNodeImpl().isDefaultCase();
    }

    @Override
    public boolean getIsEmptyImpl() {
        return this.getNodeImpl().isEmptyCase();
    }

    @Override
    public AStatement<?> getNextInstructionImpl() {
        var nextInst = this.getNodeImpl().nextExecutedInstruction();
        if (nextInst == null) {
            return null;
        }

        return CxxJoinpoints.create(nextInst, getWeaverEngine(), AStatement.class);
    }

    @Override
    public AStatement<?>[] getInstructionsImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getInstructions(), getWeaverEngine(), AStatement.class);
    }

    @Override
    public ACase<?> getNextCaseImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().nextCase(), getWeaverEngine(), ACase.class);
    }

    @Override
    public AExpression<?>[] getValuesImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getValues(), getWeaverEngine(), AExpression.class);
    }

}
