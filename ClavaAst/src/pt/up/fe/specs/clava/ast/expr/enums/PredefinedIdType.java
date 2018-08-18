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
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum PredefinedIdType implements StringProvider {
    FUNC("__func__"),
    FUNCTION("__FUNCTION__"),
    L_FUNCTION("L__FUNCTION__"),
    FUND_D_NAME("__FUNCDNAME__"),
    FUNC_SIG("__FUNCSIG__"),
    PRETTY_FUNCTION("__PRETTY_FUNCTION__"),
    PRETTY_FUNCTION_NO_VIRTUAL("<not implemented for PRETTY_FUNCTION_NO_VIRTUAL>");

    private static final Lazy<EnumHelperWithValue<PredefinedIdType>> ENUM_HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(PredefinedIdType.class);

    public static EnumHelperWithValue<PredefinedIdType> getEnumHelper() {
        return ENUM_HELPER.get();
    }

    private PredefinedIdType(String identTypeName) {
        this.identTypeName = identTypeName;
    }

    private final String identTypeName;

    @Override
    public String getString() {
        return identTypeName;
    }

}