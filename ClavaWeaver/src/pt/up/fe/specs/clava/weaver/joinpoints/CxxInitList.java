/**
 * Copyright 2024 SPeCS.
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
import pt.up.fe.specs.clava.ast.expr.InitListExpr;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AInitList;

public class CxxInitList extends AInitList {

    private final InitListExpr initList;

    public CxxInitList(InitListExpr initList) {
        super(new CxxExpression(initList));

        this.initList = initList;
    }

    @Override
    public ClavaNode getNode() {
        return initList;
    }

    @Override
    public AExpression getArrayFillerImpl() {
        return initList.get(InitListExpr.ARRAY_FILLER)
                .map(n -> CxxJoinpoints.create(n, AExpression.class))
                .orElse(null);

    }

}
