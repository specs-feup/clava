/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clava.weaver.joinpoints.types;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.ParenType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AParenType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;

public class CxxParenType extends AParenType {

    private final ParenType parenType;

    public CxxParenType(ParenType parenType) {
        super(new CxxType(parenType));

        this.parenType = parenType;
    }

    @Override
    public ClavaNode getNode() {
        return parenType;
    }

    @Override
    public AType getInnerTypeImpl() {
        return CxxJoinpoints.create(parenType.getInnerType(), AType.class);
    }

    @Override
    public void defInnerTypeImpl(AType value) {
        var newType = (Type) value.getNode();
        parenType.setInnerType(newType);
    }

    @Override
    public void setInnerTypeImpl(AType innerType) {
        defInnerTypeImpl(innerType);
    }
}
