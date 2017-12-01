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
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

public class CXXDependentScopeMemberExpr extends Expr {

    private final boolean isArrow;
    private final String memberName;

    public CXXDependentScopeMemberExpr(boolean isArrow, String memberName, ExprData exprData, ClavaNodeInfo info,
            Expr memberExpr) {
        this(isArrow, memberName, exprData, info, Arrays.asList(memberExpr));
    }

    protected CXXDependentScopeMemberExpr(boolean isArrow, String memberName, ExprData exprData,
            ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(exprData, info, children);

        this.isArrow = isArrow;
        this.memberName = memberName;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXDependentScopeMemberExpr(isArrow, memberName, getExprData(), getInfo(), Collections.emptyList());
    }

    public Expr getMemberExpr() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String getCode() {
        String separator = isArrow ? "->" : ".";
        // String separator = "->";

        return getMemberExpr().getCode() + separator + memberName;
        // System.out.println("CHILD CODE:" + getChild(0).getCode());
        // return super.getCode();
    }

}
