/**
 * Copyright 2016 SPeCS.
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

import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunctionType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;

public class CxxFunctionType<Self extends CxxFunctionType<Self>> extends AFunctionType<Self> {

    public CxxFunctionType(FunctionType type, CxxWeaver weaver) {
        super(type, weaver);
    }

    @Override
    public FunctionType getNodeImpl() {
        return (FunctionType) super.getNodeImpl();
    }

    @Override
    public AType<?> getReturnTypeImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getReturnType(), getWeaverEngine(), AType.class);
    }

    @Override
    public AType<?>[] getParamTypesImpl() {
        return this.getNodeImpl().getParamTypes().stream()
                .map(paramType -> CxxJoinpoints.create(paramType, getWeaverEngine(), AType.class))
                .toArray(AType[]::new);
    }

    @Override
    public void setReturnTypeImpl(AType<?> newType) {
        Type newClavaType = (Type) newType.getNodeImpl();
        this.getNodeImpl().set(FunctionType.RETURN_TYPE, newClavaType);
    }

    @Override
    public void setParamTypeImpl(int index, AType<?> newType) {
        this.getNodeImpl().setParamType(index, (Type) newType.getNodeImpl());
    }

}
