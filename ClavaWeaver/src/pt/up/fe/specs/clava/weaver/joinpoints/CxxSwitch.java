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

import pt.up.fe.specs.clava.ast.stmt.SwitchStmt;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACase;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ASwitch;

public class CxxSwitch<Self extends CxxSwitch<Self>> extends ASwitch<Self> {

    public CxxSwitch(SwitchStmt switchStmt, CxxWeaver weaver) {
        super(switchStmt, weaver);
    }

    @Override
    public SwitchStmt getNodeImpl() {
        return (SwitchStmt) super.getNodeImpl();
    }

    @Override
    public boolean getHasDefaultCaseImpl() {
        return this.getNodeImpl().hasDefaultCase();
    }

    @Override
    public ACase<?> getGetDefaultCaseImpl() {
        return this.getNodeImpl().getDefaultCase()
                .map(node -> CxxJoinpoints.create(node,
                        getWeaverEngine(), ACase.class))
                .orElse(null);
    }

    @Override
    public ACase<?>[] getCasesImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getCases(), getWeaverEngine(), ACase.class);
    }

    @Override
    public AExpression<?> getConditionImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getCond(), getWeaverEngine(), AExpression.class);
    }

}
