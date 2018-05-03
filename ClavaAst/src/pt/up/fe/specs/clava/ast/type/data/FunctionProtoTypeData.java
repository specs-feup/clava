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

import pt.up.fe.specs.clava.ast.type.legacy.ExceptionSpecifier;
import pt.up.fe.specs.clava.language.ReferenceQualifier;

public class FunctionProtoTypeData {

    private final boolean hasTrailingReturn;
    private final boolean isConst;
    private final boolean isVolatile;
    private final boolean isRestrict;
    private final ReferenceQualifier referenceQualifier;
    private ExceptionSpecifier specifier;
    private String noexceptExpr;

    public FunctionProtoTypeData() {
        this(false, false, false, false, ReferenceQualifier.None);
    }

    public FunctionProtoTypeData(boolean hasTrailingReturn, boolean isConst, boolean isVolatile, boolean isRestrict,
            ReferenceQualifier referenceQualifier) {

        this.hasTrailingReturn = hasTrailingReturn;
        this.isConst = isConst;
        this.isVolatile = isVolatile;
        this.isRestrict = isRestrict;
        this.referenceQualifier = referenceQualifier;
        this.specifier = ExceptionSpecifier.NONE;
        this.noexceptExpr = null;
    }

    public boolean isHasTrailingReturn() {
        return hasTrailingReturn;
    }

    public boolean isConst() {
        return isConst;
    }

    public boolean isVolatile() {
        return isVolatile;
    }

    public boolean isRestrict() {
        return isRestrict;
    }

    public ReferenceQualifier getReferenceQualifier() {
        return referenceQualifier;
    }

    public ExceptionSpecifier getSpecifier() {
        return specifier;
    }

    public String getNoexceptExpr() {
        return noexceptExpr;
    }

    public void setSpecifier(ExceptionSpecifier specifier) {
        this.specifier = specifier;
    }

    public void setNoexceptExpr(String noexceptExpr) {
        this.noexceptExpr = noexceptExpr;
    }
}
