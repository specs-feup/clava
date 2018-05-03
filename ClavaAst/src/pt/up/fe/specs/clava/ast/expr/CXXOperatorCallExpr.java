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
import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.language.CXXOperator;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public class CXXOperatorCallExpr extends CallExpr {

    public CXXOperatorCallExpr(ExprData exprData, ClavaNodeInfo info, Expr function, List<? extends Expr> args) {
        this(exprData, info, SpecsCollections.concat(function, args));
    }

    private CXXOperatorCallExpr(ExprData exprData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(exprData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXOperatorCallExpr(getExprData(), getInfo(), Collections.emptyList());
    }

    @Override
    public String getCode() {

        // Should return string representing the operator
        String operatorRefname = getCallee().getCode();

        CXXOperator cxxOperator = CXXOperator.getHelper().fromValue(operatorRefname);

        // String code = cxxOperator.getCode(getArgs());
        // if (code.startsWith("it ++")) {
        // System.out.println("CODE:" + code);
        // }

        return cxxOperator.getCode(getArgs());
    }

    @Override
    public List<String> getCallMemberNames() {
        throw new NotImplementedException(getClass());
    }

}
