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

package pt.up.fe.specs.clava.ast.extra;

import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.Expr;

public class TemplateArgumentExpr extends TemplateArgument {

    private final Expr expr;

    public TemplateArgumentExpr(Expr expr, ClavaNodeInfo nodeInfo) {
        super(nodeInfo, Collections.emptyList());

        this.expr = expr;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new TemplateArgumentExpr(expr, getInfo());
    }

    public Expr getExpr() {
        return expr;
    }

    @Override
    public String toContentString() {
        return "expr:" + getExpr().getCode();
    }

    @Override
    public String getCode() {
        // System.out.println("TEMPLATE EXPR CODE:" + getExpr().getCode());
        return expr.getCode();
    }
}
