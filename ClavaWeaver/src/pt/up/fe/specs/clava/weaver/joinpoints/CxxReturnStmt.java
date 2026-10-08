/**
 * Copyright 2017 SPeCS.
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

import pt.up.fe.specs.clava.ast.stmt.ReturnStmt;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AReturnStmt;

public class CxxReturnStmt<Self extends CxxReturnStmt<Self>> extends AReturnStmt<Self> {

    public CxxReturnStmt(ReturnStmt returnStmt, CxxWeaver weaver) {
        super(returnStmt, weaver);
    }

    @Override
    public ReturnStmt getNodeImpl() {
        return (ReturnStmt) super.getNodeImpl();
    }

    @Override
    public AExpression<?> getReturnExprImpl() {
        return this.getNodeImpl().getRetValue().map(retValue -> CxxJoinpoints.create(retValue,
                getWeaverEngine(), AExpression.class)).orElse(null);
    }

}
