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
import pt.up.fe.specs.clava.ast.stmt.SwitchStmt;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACase;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ASwitch;

public class CxxSwitch extends ASwitch {

    private final SwitchStmt switchStmt;

    public CxxSwitch(SwitchStmt switchStmt) {
        super(new CxxStatement(switchStmt));
        this.switchStmt = switchStmt;
    }

    @Override
    public ClavaNode getNode() {
        return switchStmt;
    }

    @Override
    public Boolean getHasDefaultCaseImpl() {
        return switchStmt.hasDefaultCase();
    }

    @Override
    public ACase getGetDefaultCaseImpl() {
        return switchStmt.getDefaultCase()
                .map(node -> CxxJoinpoints.create(node, ACase.class))
                .orElse(null);
    }

    @Override
    public ACase[] getCasesArrayImpl() {
        return CxxJoinpoints.create(switchStmt.getCases(), ACase.class);
    }

    @Override
    public AExpression getConditionImpl() {
        return CxxJoinpoints.create(switchStmt.getCond(), AExpression.class);
    }

}
