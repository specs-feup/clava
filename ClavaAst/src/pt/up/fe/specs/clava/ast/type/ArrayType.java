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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.data.ArrayTypeData;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.enums.ArraySizeModifier;
import pt.up.fe.specs.clava.ast.type.enums.C99Qualifier;

public abstract class ArrayType extends Type {

    /// DATAKEYS BEGIN

    public final static DataKey<ArraySizeModifier> ARRAY_SIZE_MODIFIER = KeyFactory
            .enumeration("arraySizeModifier", ArraySizeModifier.class);

    public final static DataKey<List<C99Qualifier>> INDEX_TYPE_QUALIFIERS = KeyFactory
            .generic("indexTypeQualifiers", new ArrayList<>());

    /// DATAKEYS END

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
    public String getCode(ClavaNode sourceNode, String name) {
        String nameCode = name == null ? "" : name;

        Type elementType = getElementType();

        // String qualifierString = ClavaCode.getQualifiersCode(arrayTypeData.getQualifiers(), isCxx);
        String qualifierString = arrayTypeData.getQualifiersCode();
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
            return elementType.getCode(sourceNode, nameCode + "[" + arrayContentCode + "]");
        }

        return getElementType().getCode(sourceNode) + " " + nameCode + "[" + arrayContentCode + "]";
    }

    @Override
    public boolean isArray() {
        return true;
    }

}
