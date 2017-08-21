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
import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.CXXNamedCastExprData;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

public class CXXConstCastExpr extends CXXNamedCastExpr {

    public CXXConstCastExpr(CXXNamedCastExprData cxxNamedCastExprdata, ExprData exprData, ClavaNodeInfo info,
            Expr subExpr) {

        this(cxxNamedCastExprdata, exprData, info, Arrays.asList(subExpr));
    }

    private CXXConstCastExpr(CXXNamedCastExprData cxxNamedCastExprdata, ExprData exprData, ClavaNodeInfo info,
            List<? extends ClavaNode> children) {

        super(cxxNamedCastExprdata, exprData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXConstCastExpr(getCxxNamedCastExprdata(), getExprData(), getInfo(), Collections.emptyList());
    }

}
