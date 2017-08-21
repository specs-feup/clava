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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.Arrays;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.CXXNamedCastExprData;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

public abstract class CXXNamedCastExpr extends ExplicitCastExpr {

    private final CXXNamedCastExprData cxxNamedCastExprdata;

    public CXXNamedCastExpr(CXXNamedCastExprData cxxNamedCastExprdata, ExprData exprData, ClavaNodeInfo info,
            Expr subExpr) {

        this(cxxNamedCastExprdata, exprData, info, Arrays.asList(subExpr));
    }

    /**
     * General constructor.
     * 
     * @param castKind
     * @param location
     */
    protected CXXNamedCastExpr(CXXNamedCastExprData cxxNamedCastExprdata, ExprData exprData, ClavaNodeInfo info,
            List<? extends ClavaNode> children) {

        super(cxxNamedCastExprdata.getCastKind(), exprData, info, children);

        this.cxxNamedCastExprdata = cxxNamedCastExprdata;
    }

    public CXXNamedCastExprData getCxxNamedCastExprdata() {
        return cxxNamedCastExprdata;
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append(cxxNamedCastExprdata.getCastName());
        code.append("<").append(cxxNamedCastExprdata.getTypeAsWritten()).append(">");
        code.append("(").append(getSubExpr().getCode()).append(")");

        return code.toString();

    }
}
