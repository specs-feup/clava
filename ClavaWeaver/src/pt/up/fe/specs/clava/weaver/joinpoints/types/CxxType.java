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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.Types;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.ConstantArrayType;
import pt.up.fe.specs.clava.ast.type.PointerType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;

public class CxxType extends AType {

    private final Type type;
    private final ACxxWeaverJoinPoint parent;

    public CxxType(Type type, ACxxWeaverJoinPoint parent) {
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
    public Boolean getIsArrayImpl() {
        return type.isArray();
        // return type instanceof ArrayType;
    }

    @Override
    public Integer getArraySizeImpl() {
        if (!(type instanceof ConstantArrayType)) {
            return -1;
        }

        return ((ConstantArrayType) type).getConstant();
    }

    /*
    @Override
    public AJoinPoint getElementTypeImpl() {
        if (type instanceof ArrayType) {
            return CxxJoinpoints.create(((ArrayType) type).getElementType(), this);
        }
    
        return this;
    }
    */

    @Override
    public Boolean getHasTemplateArgsImpl() {
        return type.hasTemplateArgs();
    }

    @Override
    public String[] getTemplateArgsStringsArrayImpl() {
        return type.getTemplateArgumentStrings().toArray(new String[0]);
    }

    @Override
    public Boolean getHasSugarImpl() {
        // return type.getTypeData().hasSugar();
        return type.hasSugar();
    }

    @Override
    public AJoinPoint getDesugarImpl() {
        return CxxJoinpoints.create(type.desugar(), this);
    }

    @Override
    public Boolean getIsBuiltinImpl() {
        return type instanceof BuiltinType;
    }

    @Override
    public Boolean getConstantImpl() {
        return type.isConst();
    }

    @Override
    public String getKindImpl() {
        return type.getNodeName();
    }

    @Override
    public Boolean getIsPointerImpl() {
        return type instanceof PointerType;
    }

    @Override
    public AType getUnwrapImpl() {
        Type unwrappedType = Types.getSingleElement(type);

        if (unwrappedType == null) {
            return null;
        }

        return (AType) CxxJoinpoints.create(unwrappedType, this);
    }

    @Override
    public Boolean getIsTopLevelImpl() {
        // Type is top-level if it has not parent
        return !type.hasParent();
    }

    @Override
    public AType[] getTemplateArgsTypesArrayImpl() {
        return type.getTemplateArgumentTypes().stream()
                .map(argType -> (AType) CxxJoinpoints.create(argType, this))
                .toArray(size -> new AType[size]);

    }

    @Override
    public void defTemplateArgsTypesImpl(AType[] value) {
        List<Type> argTypes = Arrays.stream(value)
                .map(aType -> (Type) aType.getNode())
                .collect(Collectors.toList());

        type.setTemplateArgumentTypes(argTypes);

    }

    @Override
    public void setTemplateArgsTypesImpl(AType[] templateArgTypes) {
        defTemplateArgsTypesImpl(templateArgTypes);
    }

    @Override
    public void setTemplateArgsTypesImpl(Integer index, AType templateArgType) {
        type.setTemplateArgumentType(index, (Type) templateArgType.getNode());
    }
}
