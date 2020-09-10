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

import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.CastExpr;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACast;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;

public class CxxCast extends ACast {

    private final CastExpr cast;

    public CxxCast(CastExpr cast) {
        super(new CxxExpression(cast));

        this.cast = cast;
    }

    @Override
    public ClavaNode getNode() {
        return cast;
    }

    @Override
    public Boolean getIsImplicitCastImpl() {
        throw new RuntimeException("cast.isImplicitCast deprecated, please use instead expr.implicitCast");
    }

    @Override
    public AJoinPoint getFromTypeImpl() {
        Type fromType = cast.getSubExpr().getType();

        return CxxJoinpoints.create(fromType);
    }

    @Override
    public AJoinPoint getToTypeImpl() {
        return CxxJoinpoints.create(cast.getCastType());
    }

    @Override
    public AVardecl getVardeclImpl() {
        return ((AExpression) CxxJoinpoints.create(cast.getSubExpr())).getVardeclImpl();
    }

    @Override
    public ADecl getDeclImpl() {
        return getVardeclImpl();
    }

    @Override
    public List<? extends AVardecl> selectVardecl() {
        return CxxExpression.selectVarDecl(this);
    }

    @Override
    public AExpression getSubExprImpl() {
        return (AExpression) CxxJoinpoints.create(cast.getSubExpr());

    }
}
