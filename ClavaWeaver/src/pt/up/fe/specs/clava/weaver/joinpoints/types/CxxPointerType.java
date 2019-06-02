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
import pt.up.fe.specs.clava.ast.type.PointerType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.APointerType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;

public class CxxPointerType extends APointerType {

    private final PointerType pointerType;

    public CxxPointerType(PointerType pointerType) {
        super(new CxxType(pointerType));

        this.pointerType = pointerType;
    }

    @Override
    public ClavaNode getNode() {
        return pointerType;
    }

    @Override
    public AType getPointeeImpl() {
        return CxxJoinpoints.create(pointerType.getPointeeType(), AType.class);
    }

    @Override
    public Integer getPointerLevelsImpl() {
        return pointerType.getPointerLevels();
    }

    @Override
    public void defPointeeImpl(AType value) {
        pointerType.set(PointerType.POINTEE_TYPE, (Type) value.getNode());
    }

    @Override
    public void setPointeeImpl(AType pointeeType) {
        defPointeeImpl(pointeeType);
    }

}
