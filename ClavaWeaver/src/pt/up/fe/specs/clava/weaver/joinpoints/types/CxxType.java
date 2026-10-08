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
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;

public class CxxType<Self extends CxxType<Self>> extends AType<Self> {

    public CxxType(Type type, CxxWeaver weaver) {
        super(type, weaver);
    }

    @Override
    public Type getNodeImpl() {
        return (Type) super.getNodeImpl();
    }

    @Override
    public boolean getIsArrayImpl() {
        return this.getNodeImpl().isArray();
    }

    @Override
    public int getArraySizeImpl() {
        if (!(this.getNodeImpl() instanceof ConstantArrayType)) {
            return -1;
        }

        return ((ConstantArrayType) this.getNodeImpl()).getArraySize();
    }

    @Override
    public int[] getArrayDimsImpl() {
        if (!(this.getNodeImpl() instanceof ArrayType)) {
            return new int[0];
        }

        return ((ArrayType) this.getNodeImpl()).getArrayDims().stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public boolean getHasTemplateArgsImpl() {
        return this.getNodeImpl().hasTemplateArgs();
    }

    @Override
    public String[] getTemplateArgsStringsImpl() {
        return this.getNodeImpl().getTemplateArgumentStrings(null).toArray(new String[0]);
    }

    @Override
    public boolean getHasSugarImpl() {
        return this.getNodeImpl().hasSugar();
    }

    @Override
    public AType<?> getDesugarImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().desugar(), getWeaverEngine(), AType.class);
    }

    @Override
    public AType<?> getDesugarAllImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().desugarAll(), getWeaverEngine(), AType.class);
    }

    @Override
    public void setDesugarImpl(AType<?> desugaredType) {
        this.getNodeImpl().setDesugar((Type) desugaredType.getNodeImpl());
    }

    @Override
    public boolean getIsBuiltinImpl() {
        return this.getNodeImpl() instanceof BuiltinType;
    }

    @Override
    public boolean getConstantImpl() {
        return this.getNodeImpl().isConst();
    }

    @Override
    public String getKindImpl() {
        return this.getNodeImpl().getNodeName();
    }

    @Override
    public boolean getIsPointerImpl() {
        return this.getNodeImpl().isPointer();
    }

    @Override
    public AType<?> getUnwrapImpl() {
        Type unwrappedType = Types.getSingleElement(this.getNodeImpl());

        if (unwrappedType == null) {
            return null;
        }

        return CxxJoinpoints.create(unwrappedType, getWeaverEngine(), AType.class);
    }

    @Override
    public boolean getIsTopLevelImpl() {
        // Type is top-level if it has not parent
        return !this.getNodeImpl().hasParent();
    }

    @Override
    public AType<?>[] getTemplateArgsTypesImpl() {
        return this.getNodeImpl().getTemplateArgumentTypes().stream()
                .map(argType -> CxxJoinpoints.create(argType, getWeaverEngine(), AType.class))
                .toArray(AType[]::new);

    }

    @Override
    public void setTemplateArgsTypesImpl(AType<?>[] templateArgTypes) {
        List<Type> argTypes = Arrays.stream(
                templateArgTypes)
                .map(aType -> (Type) aType.getNodeImpl())
                .collect(Collectors.toList());

        this.getNodeImpl().setTemplateArgumentTypes(argTypes);
    }

    @Override
    public void setTemplateArgTypeImpl(int index, AType<?> templateArgType) {
        this.getNodeImpl().setTemplateArgumentType(index, (Type) templateArgType.getNodeImpl());
    }

    @Override
    public AType<?> getNormalizeImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().normalize(), getWeaverEngine(), AType.class);
    }

    @Override
    public Map<String, AType<?>> getTypeFieldsImpl() {
        Map<String, AType<?>> typeFields = new HashMap<>();

        List<DataKey<?>> keys = this.getNodeImpl().getAllKeysWithNodes();

        for (DataKey<?> key : keys) {
            if (!this.getNodeImpl().hasValue(key)) {
                continue;
            }

            List<ClavaNode> values = this.getNodeImpl().getClavaNode(key);

            // Skip fields that contain more than one node
            if (values.size() != 1) {
                continue;
            }

            // Ignore nodes that are not types
            if (!(values.get(0) instanceof Type)) {
                continue;
            }

            typeFields.put(key.getName(), CxxJoinpoints.create(values.get(0), getWeaverEngine(), AType.class));
        }

        return typeFields;
    }

    @Override
    public boolean setTypeFieldByValueRecursiveImpl(Object currentValue, Object newValue) {
        return setTypeFieldByValueRecursiveImpl(this, currentValue, newValue, new HashSet<>());
    }

    private static boolean setTypeFieldByValueRecursiveImpl(AType<?> type, Object currentValue, Object newValue,
            Set<Type> checkedNodes) {

        // If already visited this node, return false
        if (checkedNodes.contains(type.getNodeImpl())) {
            return false;
        }
        // Otherwise, add current node
        else {
            checkedNodes.add((Type) type.getNodeImpl());
        }

        // Get keys with type fields
        Map<String, AType<?>> typeFields = type.getTypeFieldsImpl();

        // Iterate over each type field
        for (Entry<String, AType<?>> entry : typeFields.entrySet()) {

            // Found value to change, change it and return
            if (currentValue instanceof CxxType cxxType){
                if (((AType)entry.getValue()).getEqualsImpl(cxxType)) {
                    type.setValueImpl(entry.getKey(), newValue);
                    return true;
                }
            }
        }

        // Did not find a key in the current node, call the function recursively on a copy of the visited fields
        // If a field is changed, update it
        for (Entry<String, AType<?>> entry : typeFields.entrySet()) {
            AType<?> fieldTypeCopy = (AType<?>) entry.getValue().copyImpl();
            boolean changedField = setTypeFieldByValueRecursiveImpl(fieldTypeCopy, currentValue, newValue,
                    checkedNodes);

            // Update field
            if (changedField) {
                type.setValueImpl(entry.getKey(), fieldTypeCopy);
                return true;
            }
        }
        return false;
    }

    @Override
    public String getFieldTreeImpl() {
        return this.getNodeImpl().toFieldTree();
    }

    @Override
    public AType<?> setUnderlyingTypeImpl(AType<?> oldValue, AType<?> newValue) {
        return CxxJoinpoints.create(this.getNodeImpl().setUnderlyingType((Type) oldValue.getNodeImpl(), (Type) newValue.getNodeImpl()),
                getWeaverEngine(), AType.class);
    }

    @Override
    public boolean getIsAutoImpl() {
        return this.getNodeImpl().isAuto();
    }

    @Override
    public AType<?> asConstImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().asConst(), getWeaverEngine(), AType.class);
    }
}
