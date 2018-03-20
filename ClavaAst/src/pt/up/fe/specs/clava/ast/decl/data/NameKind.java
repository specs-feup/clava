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

package pt.up.fe.specs.clava.ast.decl.data;

import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum NameKind implements StringProvider {
    IDENTIFIER,
    OBJ_C_ZERO_ARG_SELECTOR,
    OBJ_C_ONE_ARG_SELECTOR,
    OBJ_C_MULTI_ARG_SELECTOR,
    CXX_CONSTRUCTOR_NAME,
    CXX_DESTRUCTOR_NAME,
    CXX_CONVERSION_FUNCTION_NAME,
    CXX_DEDUCTION_GUIDE_NAME,
    CXX_OPERATOR_NAME,
    CXX_LITERAL_OPERATOR_NAME,
    CXX_USING_DIRECTIVE;

    private static final Lazy<EnumHelper<NameKind>> ENUM_HELPER = EnumHelper.newLazyHelper(NameKind.class);

    public static EnumHelper<NameKind> getHelper() {
        return ENUM_HELPER.get();
    }

    @Override
    public String getString() {
        return name();
    }
}
