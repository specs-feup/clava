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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints.types;

import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunctionType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;

public class CxxFunctionType extends AFunctionType {

    private final FunctionType type;

    public CxxFunctionType(FunctionType type) {
        super(new CxxType(type));
        this.type = type;
    }

    @Override
    public Type getNode() {
        return type;
    }

    @Override
    public AType getReturnTypeImpl() {
        return CxxJoinpoints.create(type.getReturnType(), AType.class);
    }

    @Override
    public AType[] getParamTypesArrayImpl() {

        return type.getParamTypes().stream()
                .map(paramType -> CxxJoinpoints.create(paramType))
                .toArray(size -> new AType[size]);

    }

    @Override
    public void defReturnTypeImpl(AType value) {
        Type newClavaType = (Type) value.getNode();
        type.set(FunctionType.RETURN_TYPE, newClavaType);
    }

    @Override
    public void setReturnTypeImpl(AType newType) {
        defReturnTypeImpl(newType);
    }

    @Override
    public void setParamTypeImpl(Integer index, AType newType) {
        type.setParamType(index, (Type) newType.getNode());
    }

}
