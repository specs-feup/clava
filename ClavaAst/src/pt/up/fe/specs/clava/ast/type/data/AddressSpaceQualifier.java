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

package pt.up.fe.specs.clava.ast.type.data;

import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum AddressSpaceQualifier implements StringProvider {

    NONE,
    GLOBAL,
    LOCAL,
    CONSTANT,
    PRIVATE;

    private static final Lazy<EnumHelper<AddressSpaceQualifier>> ENUM_HELPER = EnumHelper
            .newLazyHelper(AddressSpaceQualifier.class, NONE);

    public static EnumHelper<AddressSpaceQualifier> getEnumHelper() {
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
}
