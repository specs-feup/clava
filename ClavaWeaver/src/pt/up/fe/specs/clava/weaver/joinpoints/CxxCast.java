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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.CastExpr;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACast;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;

public class CxxCast extends ACast {

    private final CastExpr cast;

    public CxxCast(CastExpr cast, CxxWeaver weaver) {
        super(new CxxExpression(cast, weaver), weaver);

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
    public AType getFromTypeImpl() {
        Type fromType = cast.getSubExpr().getType();

        return CxxJoinpoints.create(fromType, getWeaverEngine(), AType.class);
    }

    @Override
    public AType getToTypeImpl() {
        return CxxJoinpoints.create(cast.getCastType(), getWeaverEngine(), AType.class);
    }

    @Override
    public AVardecl getVardeclImpl() {
        return CxxJoinpoints.create(cast.getSubExpr(), getWeaverEngine(), AExpression.class).getVardeclImpl();
    }

    @Override
    public ADecl getDeclImpl() {
        return getVardeclImpl();
    }

    @Override
    public AExpression getSubExprImpl() {
        return CxxJoinpoints.create(cast.getSubExpr(), getWeaverEngine(), AExpression.class);

    }
}
