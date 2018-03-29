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
import pt.up.fe.specs.clava.weaver.CxxActions;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunctionType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;

public class CxxFunctionType extends AFunctionType {

    private final FunctionType type;
    private final ACxxWeaverJoinPoint parent;

    public CxxFunctionType(FunctionType type, ACxxWeaverJoinPoint parent) {
        super(new CxxType(type, parent));
        this.type = type;
        this.parent = parent;
    }

    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        return parent;
    }

    @Override
    public Type getNode() {
        return type;
    }

    @Override
    public AJoinPoint getReturnTypeImpl() {
        return CxxJoinpoints.create(type.getReturnType(), this);
    }

    @Override
    public AJoinPoint[] getParamTypesArrayImpl() {

        return type.getParamTypes().stream()
                .map(paramType -> CxxJoinpoints.create(paramType, this))
                .toArray(size -> new AJoinPoint[size]);

    }

    @Override
    public void defReturnTypeImpl(AJoinPoint value) {
        Type newClavaType = (Type) value.getNode();
        CxxActions.replace(type.getReturnType(), newClavaType, getWeaverEngine());
    }

    @Override
    public void setReturnTypeImpl(AType newType) {
        defReturnTypeImpl(newType);
    }

}
