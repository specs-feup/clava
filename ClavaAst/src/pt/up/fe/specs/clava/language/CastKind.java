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

import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum CastKind implements StringProvider {

    DEPENDENT,
    BIT_CAST,
    L_VALUE_BIT_CAST,
    L_VALUE_TO_R_VALUE,
    NO_OP,
    BASE_TO_DERIVED,
    DERIVED_TO_BASE,
    UNCHECKED_DERIVED_TO_BASE,
    DYNAMIC,
    TO_UNION,
    ARRAY_TO_POINTER_DECAY,
    FUNCTION_TO_POINTER_DECAY,
    NULL_TO_POINTER,
    NULL_TO_MEMBER_POINTER,
    BASE_TO_DERIVED_MEMBER_POINTER,
    DERIVED_TO_BASE_MEMBER_POINTER,
    MEMBER_POINTER_TO_BOOLEAN,
    REINTERPRET_MEMBER_POINTER,
    USER_DEFINED_CONVERSION,
    CONSTRUCTOR_CONVERSION,
    INTEGRAL_TO_POINTER,
    POINTER_TO_INTEGRAL,
    POINTER_TO_BOOLEAN,
    TO_VOID,
    VECTOR_SPLAT,
    INTEGRAL_CAST,
    INTEGRAL_TO_BOOLEAN,
    INTEGRAL_TO_FLOATING,
    FLOATING_TO_INTEGRAL,
    FLOATING_TO_BOOLEAN,
    BOOLEAN_TO_SIGNED_INTEGRAL,
    FLOATING_CAST,
    C_POINTER_TO_OBJ_C_POINTER_CAST,
    BLOCK_POINTER_TO_OBJ_C_POINTER_CAST,
    ANY_POINTER_TO_BLOCK_POINTER_CAST,
    OBJ_C_OBJECT_L_VALUE_CAST,
    FLOATING_REAL_TO_COMPLEX,
    FLOATING_COMPLEX_TO_REAL,
    FLOATING_COMPLEX_TO_BOOLEAN,
    FLOATING_COMPLEX_CAST,
    FLOATING_COMPLEX_TO_INTEGRAL_COMPLEX,
    INTEGRAL_REAL_TO_COMPLEX,
    INTEGRAL_COMPLEX_TO_REAL,
    INTEGRAL_COMPLEX_TO_BOOLEAN,
    INTEGRAL_COMPLEX_CAST,
    INTEGRAL_COMPLEX_TO_FLOATING_COMPLEX,
    ARC_PRODUCE_OBJECT("ARCProduceObject"),
    ARC_CONSUME_OBJECT("ARCConsumeObject"),
    ARC_RECLAIM_RETURNED_OBJECT("ARCReclaimReturnedObject"),
    ARC_EXTEND_BLOCK_OBJECT("ARCExtendBlockObject"),
    ATOMIC_TO_NON_ATOMIC,
    NON_ATOMIC_TO_ATOMIC,
    COPY_AND_AUTORELEASE_BLOCK_OBJECT,
    BUILTIN_FN_TO_FN_PTR,
    ZERO_TO_OCL_EVENT("ZeroToOCLEvent"),
    ADDRESS_SPACE_CONVERSION;

    private static final Lazy<EnumHelper<CastKind>> ENUM_HELPER = EnumHelper.newLazyHelper(CastKind.class);

    public static EnumHelper<CastKind> getHelper() {
        return ENUM_HELPER.get();
    }

    private final String string;

    private CastKind(String string) {
        this.string = string;
    }

    private CastKind() {
        this.string = SpecsStrings.toCamelCase(name(), "_", true);
    }

    @Override
    public String getString() {
        return string;
    }

}