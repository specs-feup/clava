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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.data.TypeData;

public class PointerType extends Type {

    public PointerType(TypeData typeData, ClavaNodeInfo info, Type pointeeType) {
        this(typeData, info, Arrays.asList(pointeeType));
    }

    private PointerType(TypeData typeData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(typeData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new PointerType(getTypeData(), getInfo(), Collections.emptyList());
    }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {
        Type pointeeType = getPointeeType();

        // Special case for pointers to arrays
        // http://eli.thegreenplace.net/2010/01/11/pointers-to-arrays-in-c
        if (PointerType.isPointerToParenType(this)) {
            // if (pointeeType instanceof ParenType) {
            String parsedName = name != null ? name : "";
            return ((ParenType) pointeeType).getInnerType().getCode(sourceNode, "(*" + parsedName + ")");
            // return pointeeType.getCode("*" + name);
        }

        String nameString = name == null ? "" : " " + name;
        String pointeeCode = pointeeType.getCode();
        String pointeeSuffix = pointeeCode.endsWith("*") ? "" : " ";
        return pointeeCode + pointeeSuffix + "*" + nameString;
        // return getPointeeType().getCode(name) + "*";
        // If pointee type is not a pointer, add a space
        // String space = getPointeeType() instanceof PointerType ? "" : " ";
        // return getPointeeType().getCode() + space + "*";
    }

    public Type getPointeeType() {
        return getChild(Type.class, 0);
    }

    public static boolean isPointerToParenType(Type type) {
        if (!(type instanceof PointerType)) {
            return false;
        }

        PointerType pointerType = (PointerType) type;

        return pointerType.getPointeeType() instanceof ParenType;
    }

    /**
     * 
     * @return the number of levels of this pointer. If the pointee is not a pointer, returns 1.
     */
    public int getPointerLevels() {
        Type pointee = getPointeeType();
        if (!(pointee instanceof PointerType)) {
            return 1;
        }

        return 1 + ((PointerType) pointee).getPointerLevels();
    }

    /*
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((getTypeData() == null) ? 0 : getTypeData().hashCode());
        result = prime * result + ((getPointeeType() == null) ? 0 : getPointeeType().hashCode());
        return result;
    }
    */

    /*
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
    
        PointerType other = (PointerType) obj;
    
        // Test pointee type
        if (getPointeeType() == null) {
            if (other.getPointeeType() != null) {
                return false;
            }
        } else if (!getPointeeType().equals(other.getPointeeType())) {
            return false;
        }
    
        return true;
    }
    */

}
