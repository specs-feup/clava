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

import java.util.EnumSet;
import java.util.Set;

import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum UnaryOperatorKind implements StringProvider {
    POST_INC("++"),
    POST_DEC("--"),
    PRE_INC("++"),
    PRE_DEC("--"),
    // &
    ADDR_OF("&"),
    // *
    DEREF("*"),
    PLUS("+"),
    MINUS("-"),
    NOT("~"),
    // Logical not
    L_NOT("!"),
    REAL("__real__"),
    IMAG("__imag__ "),
    EXTENSION("__extension__"),
    COAWAIT("co_await");

    private static final Lazy<EnumHelperWithValue<UnaryOperatorKind>> ENUM_HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(UnaryOperatorKind.class);

    private static final Set<UnaryOperatorKind> OPS_WITH_SPACE = EnumSet.of(REAL, IMAG, EXTENSION, COAWAIT);

    public static EnumHelperWithValue<UnaryOperatorKind> getEnumHelper() {
        return ENUM_HELPER.get();
    }

    final String op;

    private UnaryOperatorKind(String op) {
        this.op = op;
    }

    public String getCode() {
        return op;
    }

    public boolean requiresSpace() {
        return OPS_WITH_SPACE.contains(this);
    }

    public String getName() {
        return name().toLowerCase();
    }

    @Override
    public String getString() {
        return op;
    }

}