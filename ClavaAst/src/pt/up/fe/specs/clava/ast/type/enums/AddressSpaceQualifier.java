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

package pt.up.fe.specs.clava.ast.type.enums;

import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum AddressSpaceQualifier implements StringProvider {

    NONE,
    GLOBAL,
    LOCAL,
    CONSTANT,
    PRIVATE;

    private static final Lazy<EnumHelperWithValue<AddressSpaceQualifier>> ENUM_HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(AddressSpaceQualifier.class, NONE);

    public static EnumHelperWithValue<AddressSpaceQualifier> getEnumHelper() {
        return ENUM_HELPER.get();
    }

    public String getCode() {
        if (this == NONE) {
            return "";
        }

        return "__" + name().toLowerCase();
    }

    @Override
    public String getString() {
        return getCode();
    }

    public AddressSpaceQualifierV2 toV2() {
        switch (this) {
        case CONSTANT:
            return AddressSpaceQualifierV2.CONSTANT;
        case NONE:
            return AddressSpaceQualifierV2.NONE;
        case GLOBAL:
            return AddressSpaceQualifierV2.GLOBAL;
        case LOCAL:
            return AddressSpaceQualifierV2.LOCAL;
        default:
            throw new RuntimeException("Case not defined:" + this);
        }
    }
}
