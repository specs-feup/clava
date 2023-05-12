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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Types;
import pt.up.fe.specs.clava.ast.type.ArrayType;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.ConstantArrayType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;

public class CxxType extends AType {

    private final Type type;

    public CxxType(Type type) {
        this.type = type;
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

        return ((ConstantArrayType) type).getArraySize();
    }

    @Override
    public Integer[] getArrayDimsArrayImpl() {
        if (!(type instanceof ArrayType)) {
            return new Integer[0];
        }

        return ((ArrayType) type).getArrayDims().toArray(new Integer[0]);
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
        return type.getTemplateArgumentStrings(null).toArray(new String[0]);
    }

    @Override
    public Boolean getHasSugarImpl() {
        // return type.getTypeData().hasSugar();
        return type.hasSugar();

    }

    @Override
    public AType getDesugarImpl() {
        return CxxJoinpoints.create(type.desugar(), AType.class);
    }

    @Override
    public AType getDesugarAllImpl() {
        return CxxJoinpoints.create(type.desugarAll(), AType.class);
    }

    @Override
    public void defDesugarImpl(AType value) {
        type.setDesugar((Type) value.getNode());
    }

    @Override
    public void setDesugarImpl(AType desugaredType) {
        defDesugarImpl(desugaredType);
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
        return type.isPointer();
        // return type instanceof PointerType;
    }

    @Override
    public AType getUnwrapImpl() {
        Type unwrappedType = Types.getSingleElement(type);

        if (unwrappedType == null) {
            return null;
        }

        return (AType) CxxJoinpoints.create(unwrappedType);
    }

    @Override
    public Boolean getIsTopLevelImpl() {
        // Type is top-level if it has not parent
        return !type.hasParent();
    }

    @Override
    public AType[] getTemplateArgsTypesArrayImpl() {
        return type.getTemplateArgumentTypes().stream()
                .map(argType -> (AType) CxxJoinpoints.create(argType))
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

    @Override
    public AType getNormalizeImpl() {
        return CxxJoinpoints.create(type.normalize(), AType.class);
    }

    @Override
    public Map<String, AType> getTypeFieldsImpl() {
        Map<String, AType> typeFields = new HashMap<>();

        List<DataKey<?>> keys = type.getAllKeysWithNodes();

        for (DataKey<?> key : keys) {
            if (!type.hasValue(key)) {
                continue;
            }

            List<ClavaNode> values = type.getClavaNode(key);

            // Skip fields that contain more than one node
            if (values.size() != 1) {
                continue;
            }

            // Ignore nodes that are not types
            if (!(values.get(0) instanceof Type)) {
                continue;
            }

            typeFields.put(key.getName(), CxxJoinpoints.create(values.get(0), AType.class));
        }

        return typeFields;
    }

    @Override
    public boolean setTypeFieldByValueRecursiveImpl(Object currentValue, Object newValue) {
        return setTypeFieldByValueRecursiveImpl(this, currentValue, newValue, new HashSet<>());
    }

    private static boolean setTypeFieldByValueRecursiveImpl(AType type, Object currentValue, Object newValue,
            Set<Type> checkedNodes) {

        // If already visited this node, return false
        if (checkedNodes.contains(type.getNode())) {
            return false;
        }
        // Otherwise, add current node
        else {
            checkedNodes.add((Type) type.getNode());
        }

        // Get keys with type fields
        @SuppressWarnings("unchecked")
        Map<String, AType> typeFields = (Map<String, AType>) type.getTypeFieldsImpl();

        List<AType> visitedTypes = new ArrayList<>();

        // Iterate over each type field
        for (Entry<String, AType> entry : typeFields.entrySet()) {

            // Found value to change, change it and return
            if (entry.getValue().equals(currentValue)) {
                // System.out.println("SETTING " + newValue.getClass() + " to " + entry);
                // System.out.println(
                // "1.Replacing " + entry.getKey() + " with value " + entry.getValue().getNode().toTree()
                // + " with "
                // + ((AType) newValue).getNode().toTree());
                type.setValueImpl(entry.getKey(), newValue);
                return true;
            }

            visitedTypes.add(entry.getValue());
        }

        // Did not find a key in the current node, call the function recursively on a copy of the visited fields
        // If a field is changed, update it
        for (Entry<String, AType> entry : typeFields.entrySet()) {
            AType fieldTypeCopy = (AType) entry.getValue().copy();
            boolean changedField = setTypeFieldByValueRecursiveImpl(fieldTypeCopy, currentValue, newValue,
                    checkedNodes);

            // Update field
            if (changedField) {
                // System.out.println(
                // "2.Replacing " + entry.getKey() + " with value " + entry.getValue().getNode().toTree()
                // + " with " + fieldTypeCopy.getNode().toTree());
                type.setValue(entry.getKey(), fieldTypeCopy);
                return true;
            }
        }
        return false;
    }

    @Override
    public String getFieldTreeImpl() {
        return type.toFieldTree();
    }

    @Override
    public AType setUnderlyingTypeImpl(AType oldValue, AType newValue) {
        return CxxJoinpoints.create(type.setUnderlyingType((Type) oldValue.getNode(), (Type) newValue.getNode()),
                AType.class);
    }

    @Override
    public Integer bitWidthImpl(AJoinPoint reference) {
        var bitwidth = type.getBitwidth(reference.getNode());

        return bitwidth != -1 ? bitwidth : null;
    }

    @Override
    public Boolean getIsAutoImpl() {
        return type.isAuto();
    }

    @Override
    public AType asConstImpl() {
        return CxxJoinpoints.create(type.asConst(), AType.class);
    }
}
