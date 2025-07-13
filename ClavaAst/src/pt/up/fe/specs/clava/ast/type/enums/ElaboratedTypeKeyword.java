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

import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum ElaboratedTypeKeyword implements StringProvider {
    STRUCT,
    INTERFACE,
    UNION,
    CLASS,
    ENUM,
    TYPENAME,
    NONE;

    private static final Lazy<EnumHelperWithValue<ElaboratedTypeKeyword>> HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(ElaboratedTypeKeyword.class);

    public static EnumHelperWithValue<ElaboratedTypeKeyword> getHelper() {
        return HELPER.get();
    }

    public String getCode() {
        if (this == NONE) {
            return "";
        }

        return name().toLowerCase();
    }

    @Override
    public String getString() {
        return SpecsStrings.toCamelCase(name());
    }
}