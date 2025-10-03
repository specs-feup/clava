/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clava.weaver.joinpoints.types;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.VariableArrayType;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVariableArrayType;

public class CxxVariableArrayType extends AVariableArrayType {

    private final VariableArrayType arrayType;

    public CxxVariableArrayType(VariableArrayType arrayType) {
        super(new CxxArrayType(arrayType));

        this.arrayType = arrayType;
    }

    @Override
    public ClavaNode getNode() {
        return arrayType;
    }

    @Override
    public AExpression getSizeExprImpl() {
        return (AExpression) CxxJoinpoints.create(arrayType.get(VariableArrayType.SIZE_EXPR));
    }

    @Override
    public void setSizeExprImpl(AExpression sizeExpr) {
        arrayType.set(VariableArrayType.SIZE_EXPR, (Expr) sizeExpr.getNode());
    }

}
