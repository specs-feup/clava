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

package pt.up.fe.specs.clava.ast.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.enums.ArraySizeModifier;
import pt.up.fe.specs.clava.ast.type.enums.C99Qualifier;

public abstract class ArrayType extends ArrayTypeBase {

    // Data declarations are generated from the wire schema in ArrayTypeBase.

    public ArrayType(DataStore data, Collection<? extends ClavaNode> children, ArrayTypeBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public ArrayType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Type getElementType() {
        return getWireElementType();
    }

    public void setElementType(Type type) {
        set(ELEMENT_TYPE, type);
    }

    /**
     *
     * @return the core refering just to the array part (e.g., 2 in a ConstantArrayType)
     */
    abstract protected String getArrayCode();

    /**
     * 
     * @return an integer with the size of the array, of -1 if the size is not fixed and/or available statically
     */
    public int getConstant() {
        return -1;
    }

    public List<Integer> getArrayDims() {

        List<Integer> dims = new ArrayList<>();
        getArrayDims(dims);
        return dims;
    }

    private void getArrayDims(List<Integer> dims) {
        dims.add(getConstant());

        var elementType = getElementType();

        if (!(elementType instanceof ArrayType)) {
            return;
        }

        ((ArrayType) elementType).getArrayDims(dims);
    }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {
        String nameCode = name == null ? "" : name;

        Type elementType = getElementType();

        String qualifierString = getWireIndexTypeQualifiers().stream()
                .map(C99Qualifier::getCode)
                .collect(Collectors.joining(" "));

        String arraySizeType = getWireArraySizeModifier().getCode();

        StringBuilder arrayContentCode = new StringBuilder(qualifierString);
        if (!qualifierString.isEmpty() && !arraySizeType.isEmpty()) {
            arrayContentCode.append(' ');
        }
        arrayContentCode.append(arraySizeType);
        if (arrayContentCode.length() != 0) {
            arrayContentCode.append(' ');
        }

        arrayContentCode.append(getArrayCode());

        // If element type is itself an ArrayType, put this array code in front of the name
        if (elementType instanceof ArrayType) {
            return elementType.getCode(sourceNode, nameCode + "[" + arrayContentCode + "]");
        }

        String constantArray = "[" + arrayContentCode + "]";

        if (nameCode.isEmpty()) {
            return getElementType().getCode(sourceNode) + constantArray;
        }

        return getElementType().getCode(sourceNode, nameCode + constantArray);
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    protected List<DataKey<Type>> getUnderlyingTypeKeys() {
        return Arrays.asList(ELEMENT_TYPE);
    }

    @Override
    public boolean isConst() {
        return getWireElementType().isConst();
    }

    @Override
    public void removeConst() {
        getWireElementType().removeConst();
    }

    @Override
    public Type asConst() {
        var typeCopy = copy();

        typeCopy.set(ELEMENT_TYPE, getWireElementType().asConst());

        return typeCopy;
    }
}
