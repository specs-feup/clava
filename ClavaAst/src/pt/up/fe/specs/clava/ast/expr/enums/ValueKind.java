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

package pt.up.fe.specs.clava.ast.expr.enums;

import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum ValueKind implements StringProvider {
    /**
     * An expression that produces a temporary value that noes not have an identity, but that can be moved (the same as
     * pr-value, in C++11).
     */
    R_VALUE("rvalue"),
    /**
     * An expression which references an object with identity but that cannot be moved.
     */
    L_VALUE("lvalue"),
    /**
     * An expression which references an object with identity and that can be moved.
     */
    X_VALUE("xvalue");

    private static final Lazy<EnumHelper<ValueKind>> ENUM_HELPER = EnumHelper.newLazyHelper(ValueKind.class);

    public static EnumHelper<ValueKind> getEnumHelper() {
        return ENUM_HELPER.get();
    }

    private final String code;

    private ValueKind(String code) {
        this.code = code;
    }

    /**
     * 
     * @return returns R_VALUE
     */
    public static ValueKind getDefault() {
        return R_VALUE;
    }

    @Override
    public String getString() {
        return code;
    }
}