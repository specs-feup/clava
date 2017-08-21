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

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;

import pt.up.fe.specs.clava.ClavaCode;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.data.ArrayTypeData;
import pt.up.fe.specs.clava.ast.type.data.TypeData;

public abstract class ArrayType extends Type {

    private final ArrayTypeData arrayTypeData;

    protected ArrayType(ArrayTypeData arrayTypeData, TypeData typeData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(typeData, info, children);

        this.arrayTypeData = arrayTypeData;
    }

    abstract public Type getElementType();

    /**
     *
     * @return the core refering just to the array part (e.g., 2 in a ConstantArrayType)
     */
    abstract protected String getArrayCode();

    public ArrayTypeData getArrayTypeData() {
        return arrayTypeData;
    }

    @Override
    public String getCode(String name) {
        String nameCode = name == null ? "" : name;

        Type elementType = getElementType();

        String qualifierString = ClavaCode.getQualifiersCode(arrayTypeData.getQualifiers());
        String arraySizeType = arrayTypeData.getArraySizeType().getCode();

        StringBuilder arrayContentCode = new StringBuilder(qualifierString);
        if (!qualifierString.isEmpty() && !arraySizeType.isEmpty()) {
            arrayContentCode.append(' ');
        }
        arrayContentCode.append(arraySizeType);
        if (arrayContentCode.length() != 0) {
            arrayContentCode.append(' ');
        }

        arrayContentCode.append(getArrayCode());
        // String Arrays.asList(qualifierString,
        // arraySizeType).stream().filter(String::isEmpty).collect(Collectors.joining(" "));

        // If element type is itself an ArrayType, put this array code in front of the name
        if (elementType instanceof ArrayType) {
            return elementType.getCode(nameCode + "[" + arrayContentCode + "]");
        }

        return getElementType().getCode() + " " + nameCode + "[" + arrayContentCode + "]";
    }

    @Override
    public boolean isArray() {
        return true;
    }
}
