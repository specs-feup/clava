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

package pt.up.fe.specs.clava.ast.expr.data;

import pt.up.fe.specs.clava.ast.decl.data.BareDeclData;
import pt.up.fe.specs.clava.ast.expr.Expr;

public class InitListExprData {

    private final boolean hasInitializedFieldInUnion;
    private final Expr arrayFiller;
    private final BareDeclData fieldData;
    private final boolean isExplicit;

    public InitListExprData(boolean hasInitializedFieldInUnion, Expr arrayFiller, BareDeclData fieldData,
            boolean isExplicit) {

        this.hasInitializedFieldInUnion = hasInitializedFieldInUnion;
        this.arrayFiller = arrayFiller;
        this.fieldData = fieldData;
        this.isExplicit = isExplicit;
    }

    public boolean isHasInitializedFieldInUnion() {
        return hasInitializedFieldInUnion;
    }

    public Expr getArrayFiller() {
        return arrayFiller;
    }

    public boolean hasArrayFiller() {
        return arrayFiller != null;
    }

    public BareDeclData getFieldData() {
        return fieldData;
    }

    public boolean isExplicit() {
        return isExplicit;
    }

    @Override
    public String toString() {
        return "hasInitializedFieldInUnion: " + hasInitializedFieldInUnion + ", fieldData: " + fieldData
                + ", isExplicit: " + isExplicit;
    }

}
