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

package pt.up.fe.specs.clava.ast.type.enums;

import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum C99Qualifier implements StringProvider {

    CONST,
    RESTRICT,
    RESTRICT_C99,
    VOLATILE;

    private static final Lazy<EnumHelperWithValue<C99Qualifier>> ENUM_HELPER = EnumHelperWithValue.newLazyHelper(C99Qualifier.class);

    public static EnumHelperWithValue<C99Qualifier> getHelper() {
        return ENUM_HELPER.get();
    }

    @Override
    public String getString() {
        return name();
    }

    public String getCode() {
        switch (this) {
        case RESTRICT:
            return "__restrict";
        case RESTRICT_C99:
            return "restrict";
        default:
            return name().toLowerCase();
        }
    }
}
