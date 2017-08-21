/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clava.ast.type.data;

public class TypeData {

    private final String bareType;
    private final boolean hasSugar;
    private final boolean isDependent;
    private final TypeDependency typeDependency;
    private final boolean isVariablyModified;
    private final boolean containsUnexpandedParameterPack;
    private final boolean isFromAst;

    /**
     * Helper method for cases when we just have the bare type string.
     * 
     * @param bareType
     */
    public TypeData(String bareType) {
        this(bareType, false, false, TypeDependency.NONE, false, false, false);
    }

    public TypeData(String bareType, TypeData data) {
        this(bareType, data.hasSugar, data.isDependent, data.typeDependency, data.isVariablyModified,
                data.containsUnexpandedParameterPack, data.isFromAst);
    }

    public TypeData(String bareType, boolean hasSugar, boolean isDependent,
            TypeDependency typeDependency, boolean isVariablyModified,
            boolean containsUnexpandedParameterPack, boolean isFromAst) {
        this.bareType = bareType;
        this.hasSugar = hasSugar;
        this.isDependent = isDependent;
        this.typeDependency = typeDependency;
        this.isVariablyModified = isVariablyModified;
        this.containsUnexpandedParameterPack = containsUnexpandedParameterPack;
        this.isFromAst = isFromAst;
    }

    public String getBareType() {
        return bareType;
    }

    public boolean hasSugar() {
        return hasSugar;
    }

    public boolean isDependent() {
        return isDependent;
    }

    public TypeDependency getTypeDependency() {
        return typeDependency;
    }

    public boolean isVariablyModified() {
        return isVariablyModified;
    }

    public boolean containsUnexpandedParameterPack() {
        return containsUnexpandedParameterPack;
    }

    public boolean isFromAst() {
        return isFromAst;
    }

    /**
     * Number of expected children, just from the base TypeData.
     * 
     * @return
     */
    public int getNumBaseChildren() {
        int counter = 0;

        if (hasSugar) {
            counter++;
        }

        return counter;
    }

}
