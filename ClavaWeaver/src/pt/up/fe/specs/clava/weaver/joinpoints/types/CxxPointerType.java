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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints.types;

import pt.up.fe.specs.clava.ast.type.PointerType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.APointerType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;

public class CxxPointerType<Self extends CxxPointerType<Self>> extends APointerType<Self> {

    public CxxPointerType(PointerType pointerType, CxxWeaver weaver) {
        super(pointerType, weaver);
    }

    @Override
    public PointerType getNodeImpl() {
        return (PointerType) super.getNodeImpl();
    }

    @Override
    public AType<?> getPointeeImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getPointeeType(), getWeaverEngine(), AType.class);
    }

    @Override
    public int getPointerLevelsImpl() {
        return this.getNodeImpl().getPointerLevels();
    }

    @Override
    public void setPointeeImpl(AType<?> pointeeType) {
        this.getNodeImpl().set(PointerType.POINTEE_TYPE, (Type) pointeeType.getNodeImpl());
    }

}
