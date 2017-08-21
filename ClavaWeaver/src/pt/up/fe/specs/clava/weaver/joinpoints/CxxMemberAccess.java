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
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMemberAccess;

public class CxxMemberAccess extends AMemberAccess {

    private final MemberExpr memberExpr;
    private final ACxxWeaverJoinPoint parent;

    public CxxMemberAccess(MemberExpr memberExpr, ACxxWeaverJoinPoint parent) {
        super(new CxxExpression(memberExpr, parent));
        this.memberExpr = memberExpr;
        this.parent = parent;
    }

    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        return parent;
    }

    @Override
    public ClavaNode getNode() {
        return memberExpr;
    }

    @Override
    public AJoinPoint getBaseImpl() {
        return CxxJoinpoints.create(ClavaNodes.normalize(memberExpr.getBase()), this);
    }

    @Override
    public String getNameImpl() {
        return memberExpr.getMemberName();
    }

    @Override
    public String[] getMemberChainArrayImpl() {
        return memberExpr.getChain().toArray(new String[0]);
    }
}
