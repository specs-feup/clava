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

package pt.up.fe.specs.clava.ast.expr.enums;

import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum LambdaCaptureKind implements StringProvider {

    This, // Capturing the this object by reference
    StarThis, // Capturing the this object by copy
    ByCopy, // Capturing by copy (a.k.a., by value)
    ByRef, // Capturing by reference
    VLAType; // Capturing variable-length array type

    private static final Lazy<EnumHelperWithValue<LambdaCaptureKind>> ENUM_HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(LambdaCaptureKind.class);

    public static EnumHelperWithValue<LambdaCaptureKind> getHelper() {
        return ENUM_HELPER.get();
    }

    @Override
    public String getString() {
        return name();
    }

    public String getCode(String exprCode) {
        switch (this) {
        case This:
            if (exprCode.equals("this")) {
                return "this";
            }
            // Does this happen?
            return "this." + exprCode;
        case ByCopy:
            return exprCode;
        case ByRef:
            return "&" + exprCode;
        default:
            throw new NotImplementedException(this);
        }
    }
}
