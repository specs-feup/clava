/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clava.ast.type.data2;

import pt.up.fe.specs.clava.ast.ClavaData;

public class TypeDataV2 extends ClavaData {

    // public static TypeDataV2 empty() {
    // return empty(ClavaData.empty());
    // }

    public static TypeDataV2 empty(ClavaData data) {
        if (data instanceof TypeDataV2) {
            return (TypeDataV2) data;
        }

        return new TypeDataV2(data);
    }

    public TypeDataV2(ClavaData clavaData) {
        super(clavaData);

    }

    // public TypeDataV2(TypeDataV2 data) {
    // this(data);
    // }

    @Override
    public String toString() {

        return toString(super.toString(), "");
    }

    /**
     * Copies the current instance and sets the id of the new instance. Returns the copy.
     */
    // @Override
    // public TypeDataV2 setId(String newId) {
    // TypeDataV2 copy = ClavaData.copy(this);
    // copy.superSetId(newId);
    // return copy;
    // }

    // private void superSetId(String newId) {
    // super.setId(newId);
    // }
}
