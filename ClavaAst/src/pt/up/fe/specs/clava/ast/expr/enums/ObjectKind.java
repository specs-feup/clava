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

import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum ObjectKind implements StringProvider {
    ORDINARY("ordinary"),
    BIT_FIELD("bitfield"),
    OBJ_C_PROPERTY("objcproperty"),
    OBJ_C_SUBSCRIPT("objcsubscript"),
    VECTOR_COMPONENT("vectorcomponent");

    private static final Lazy<EnumHelperWithValue<ObjectKind>> ENUM_HELPER = EnumHelperWithValue.newLazyHelperWithValue(ObjectKind.class,
            ORDINARY);

    public static EnumHelperWithValue<ObjectKind> getEnumHelper() {
        return ENUM_HELPER.get();
    }

    private final String code;

    private ObjectKind(String code) {
        this.code = code;
    }

    /**
     * 
     * @return returns R_VALUE
     */
    public static ObjectKind getDefault() {
        return ORDINARY;
    }

    @Override
    public String getString() {
        return code;
    }
}