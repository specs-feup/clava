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

package pt.up.fe.specs.clava.ast.expr.data.lambda;

import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum LambdaCaptureKind implements StringProvider {

    THIS,
    BY_COPY,
    BY_REF,
    VLA_TYPE;

    private static final Lazy<EnumHelper<LambdaCaptureKind>> ENUM_HELPER = EnumHelper
            .newLazyHelper(LambdaCaptureKind.class);

    public static EnumHelper<LambdaCaptureKind> getHelper() {
        return ENUM_HELPER.get();
    }

    @Override
    public String getString() {
        return name();
    }

    public String getCode(String exprCode) {
        switch (this) {
        case THIS:
            if (exprCode.equals("this")) {
                return "this";
            }
            // Does this happen?
            return "this." + exprCode;
        case BY_COPY:
            return exprCode;
        case BY_REF:
            return "&" + exprCode;
        default:
            throw new RuntimeException("Not implemented for case " + this);
        }
    }
}
