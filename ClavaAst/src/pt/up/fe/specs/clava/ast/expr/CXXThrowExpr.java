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

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.SpecsCollections;

public class CXXThrowExpr extends Expr {

    public CXXThrowExpr(ExprData exprData, ClavaNodeInfo info, Expr throwExpr) {
        this(exprData, info, SpecsCollections.ofNullable(throwExpr));
    }

    private CXXThrowExpr(ExprData exprData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(exprData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXThrowExpr(getExprData(), getInfo(), Collections.emptyList());
    }

    public Optional<Expr> getThrowExpr() {
        if (!hasChildren()) {
            return Optional.empty();
        }

        return Optional.of(getChild(Expr.class, 0));
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append("throw");

        getThrowExpr().ifPresent(expr -> code.append(" ").append(expr.getCode()));

        return code.toString();
    }
}
