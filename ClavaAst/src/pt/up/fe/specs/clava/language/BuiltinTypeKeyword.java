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

package pt.up.fe.specs.clava.language;

import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum BuiltinTypeKeyword implements StringProvider {
    SIGNED,
    UNSIGNED,
    CHAR,
    WCHAR_T,
    CHAR16_T,
    CHAR32_T,
    INT,
    SHORT,
    LONG,
    FLOAT,
    DOUBLE,
    LONG_DOUBLE("long double"),
    BOOL_C99("_Bool"),
    BOOL_CXX("bool"),
    NULLPTR("nullptr_t"),
    VOID;

    private static final Lazy<EnumHelper<BuiltinTypeKeyword>> HELPER = EnumHelper
            .newLazyHelper(BuiltinTypeKeyword.class);

    public static EnumHelper<BuiltinTypeKeyword> getHelper() {
        return HELPER.get();
    }

    private final String code;

    private BuiltinTypeKeyword() {
        this.code = name().toLowerCase();
    }

    private BuiltinTypeKeyword(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String getString() {
        return getCode();
    }

}