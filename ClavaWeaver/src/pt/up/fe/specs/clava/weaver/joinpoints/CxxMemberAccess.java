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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.expr.MemberExpr;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMemberAccess;

public class CxxMemberAccess<Self extends CxxMemberAccess<Self>> extends AMemberAccess<Self> {

    public CxxMemberAccess(MemberExpr memberExpr, CxxWeaver weaver) {
        super(memberExpr, weaver);
    }

    @Override
    public MemberExpr getNodeImpl() {
        return (MemberExpr) super.getNodeImpl();
    }

    @Override
    public AExpression<?> getBaseImpl() {
        return CxxJoinpoints.create(ClavaNodes.normalize(this.getNodeImpl().getBase()), getWeaverEngine(), AExpression.class);
    }

    @Override
    public String getNameImpl() {
        return this.getNodeImpl().getMemberName();
    }

    @Override
    public AExpression<?>[] getMemberChainImpl() {
        return this.getNodeImpl().getExprChain().stream()
                .map(member -> CxxJoinpoints.create(member, getWeaverEngine(), AExpression.class))
                .toArray(size -> new AExpression[size]);
    }

    @Override
    public String[] getMemberChainNamesImpl() {
        return this.getNodeImpl().getChain().toArray(new String[0]);
    }

    @Override
    public ADecl<?> getDeclImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().get(MemberExpr.MEMBER_DECL), getWeaverEngine(), ADecl.class);
    }

    @Override
    public boolean getArrowImpl() {
        return this.getNodeImpl().get(MemberExpr.IS_ARROW);
    }

    @Override
    public void setArrowImpl(boolean isArrow) {
        this.getNodeImpl().set(MemberExpr.IS_ARROW, isArrow);
    }

}
