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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ast.expr.CXXThisExpr;
import pt.up.fe.specs.clava.ast.type.TagType;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.APointerType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AThis;

public class CxxThis<Self extends CxxThis<Self>> extends AThis<Self> {

    public CxxThis(CXXThisExpr thisExpr, CxxWeaver weaver) {
        super(thisExpr, weaver);
    }

    @Override
    public CXXThisExpr getNodeImpl() {
        return (CXXThisExpr) super.getNodeImpl();
    }

    @Override
    public ADecl<?> getDeclImpl() {
        // type.pointee.decl

        var type = getTypeImpl();

        if (!(type instanceof APointerType)) {
            throw new RuntimeException("Not implemented with type is " + type.joinPointType());
        }

        // Get class type
        var pointeeType = ((APointerType<?>) type).getPointeeImpl();

        var thisType = pointeeType.getNodeImpl();

        if (!(thisType instanceof TagType)) {
            throw new RuntimeException("Not implemented when this type is a " + thisType.getClass());
        }

        var typeDecl = thisType.get(TagType.DECL);

        return CxxJoinpoints.create(typeDecl, getWeaverEngine(), ADecl.class);
    }
}
