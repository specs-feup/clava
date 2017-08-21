/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;

import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.CXXConstructExprData;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

/**
 * Represents a C++ functional cast expression with a number of arguments different than one, that builds a temporary
 * object.
 * 
 * @author JoaoBispo
 *
 */
public class CXXTemporaryObjectExpr extends CXXConstructExpr {

    public CXXTemporaryObjectExpr(CXXConstructExprData constructorData, ExprData exprData, ClavaNodeInfo info,
            Collection<? extends Expr> args) {

        super(constructorData, exprData, info, args);
    }

    @Override
    protected boolean isTemporary() {
        return true;
    }

}
