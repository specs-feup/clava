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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.BareDeclData;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.SpecsLogs;

public class CXXDeleteExpr extends Expr {

    private final boolean isGlobal;
    private final boolean isArray;
    private final BareDeclData operatorDelete;

    public CXXDeleteExpr(boolean isGlobal, boolean isArray, BareDeclData operatorDelete, ExprData exprData,
            ClavaNodeInfo info, Expr argument) {

        this(isGlobal, isArray, operatorDelete, exprData, info, Arrays.asList(argument));
    }

    private CXXDeleteExpr(boolean isGlobal, boolean isArray, BareDeclData bareDecl, ExprData exprData,
            ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(exprData, info, children);

        this.isGlobal = isGlobal;
        this.isArray = isArray;
        this.operatorDelete = bareDecl;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXDeleteExpr(isGlobal, isArray, operatorDelete, getExprData(), getInfo(),
                Collections.emptyList());
    }

    public Expr getArgument() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        if (isGlobal) {
            SpecsLogs.msgWarn("Code generation not implemented yet when global is true");
        }

        code.append("delete");
        if (isArray) {
            code.append("[]");
        }
        code.append(" ");
        code.append(getArgument().getCode());

        return code.toString();
    }
}
