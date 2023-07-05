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

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.expr.MemberExpr;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMemberAccess;

public class CxxMemberAccess extends AMemberAccess {

    private final MemberExpr memberExpr;

    public CxxMemberAccess(MemberExpr memberExpr) {
        super(new CxxExpression(memberExpr));
        this.memberExpr = memberExpr;
    }

    @Override
    public ClavaNode getNode() {
        return memberExpr;
    }

    @Override
    public AExpression getBaseImpl() {
        return CxxJoinpoints.create(ClavaNodes.normalize(memberExpr.getBase()), AExpression.class);
    }

    @Override
    public String getNameImpl() {
        return memberExpr.getMemberName();
    }

    @Override
    public AExpression[] getMemberChainArrayImpl() {
        return memberExpr.getExprChain().stream()
                .map(member -> (AExpression) CxxJoinpoints.create(member))
                .toArray(size -> new AExpression[size]);
    }

    @Override
    public String[] getMemberChainNamesArrayImpl() {
        return memberExpr.getChain().toArray(new String[0]);
    }

    @Override
    public ADecl getDeclImpl() {
        return CxxJoinpoints.create(memberExpr.get(MemberExpr.MEMBER_DECL), ADecl.class);
    }

    @Override
    public Boolean getArrowImpl() {
        return memberExpr.get(MemberExpr.IS_ARROW);
    }

    @Override
    public void defArrowImpl(Boolean value) {
        memberExpr.set(MemberExpr.IS_ARROW, value);
    }

    @Override
    public void setArrowImpl(Boolean isArrow) {
        defArrowImpl(isArrow);
    }

}
