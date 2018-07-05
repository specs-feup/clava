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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.data.OffsetOfData;
import pt.up.fe.specs.clava.ast.expr.data.offsetof.OffsetOfComponent;

public class OffsetOfExpr extends Expr {

    private final OffsetOfData offsetOfData;

    public OffsetOfExpr(OffsetOfData offsetOfData, ExprData exprData, ClavaNodeInfo info) {
        this(offsetOfData, exprData, info, Collections.emptyList());
    }

    private OffsetOfExpr(OffsetOfData offsetOfData, ExprData exprData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(exprData, info, children);

        this.offsetOfData = offsetOfData;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new OffsetOfExpr(offsetOfData, getExprData(), getInfo(), Collections.emptyList());
    }

    public Expr getSubExpr() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String getCode() {
        String componentCode = getComponentsCode();
        return "offsetof(" + offsetOfData.getSourceType().getCode(this) + ", " + componentCode + ")";
        // return getSubExpr().getCode();
        /*
        if (!hasChildren()) {
            return "";
        }
        
        if (numChildren() == 1) {
            return getChild(0).getCode();
        }
        
        throw new RuntimeException("What to do when OffsetOfExpr has more than one child?");
        */
    }

    private String getComponentsCode() {

        StringBuilder code = new StringBuilder();

        boolean isFirst = true;
        for (OffsetOfComponent component : offsetOfData.getComponents()) {

            if (component.isField() && !isFirst) {
                code.append(".");
            }

            code.append(component.getCode());

            if (isFirst) {
                isFirst = false;
            }
        }
        return code.toString();
    }
}
