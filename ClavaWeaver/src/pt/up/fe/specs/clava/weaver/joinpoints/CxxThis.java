/**
 * Copyright 2020 SPeCS.
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
import pt.up.fe.specs.clava.ast.expr.CXXThisExpr;
import pt.up.fe.specs.clava.ast.type.TagType;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.APointerType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AThis;

public class CxxThis extends AThis {

    private final CXXThisExpr thisExpr;

    public CxxThis(CXXThisExpr thisExpr) {
        super(new CxxExpression(thisExpr));
        this.thisExpr = thisExpr;
    }

    @Override
    public ClavaNode getNode() {
        return thisExpr;
    }

    @Override
    public ADecl getDeclImpl() {
        // type.pointee.decl

        var type = getTypeImpl();

        if (!(type instanceof APointerType)) {
            throw new RuntimeException("Not implemented with type is " + type.getJoinPointType());
        }

        // Get class type
        var pointeeType = ((APointerType) type).getPointeeImpl();

        var thisType = pointeeType.getNode();

        if (!(thisType instanceof TagType)) {
            throw new RuntimeException("Not implemented when this type is a " + thisType.getClass());
        }

        var typeDecl = thisType.get(TagType.DECL);

        return CxxJoinpoints.create(typeDecl, ADecl.class);
    }
}
