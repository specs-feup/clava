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

import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum BuiltinKind implements StringProvider {

    VOID,
    BOOL,
    CHAR,
    UNSIGNED_CHAR("unsigned char"),
    WCHAR_T("wchar_t"),
    CHAR16_T("char16_t"),
    CHAR32_T("char32_t"),
    UNSIGNED_SHORT("unsigned short"),
    UNSIGNED_INT("unsigned int"),
    UNSIGNED_LONG("unsigned long"),
    UNSIGNED_LONG_LONG("unsigned long long"),
    UINT128_T("_uint128_t"),
    CHAR_SIGNED("char"),
    CHAR_SIGNED_EXPLICIT("signed char"),
    WCHAR_T_SIGNED("wchar_t"),
    SHORT("short"),
    INT("int"),
    LONG("long"),
    LONG_LONG("long long"),
    INT128_T("__int128_t"),
    HALF("half"),
    FLOAT("float"),
    DOUBLE("double"),
    LONG_DOUBLE("long double"),
    FLOAT16("_Float16"),
    FLOAT128("__float128"),
    NULLPTR,
    OBJ_C_ID("id"),
    OBJ_C_CLASS("Class"),
    OBJ_C_SEL("SEL"),
    OPENCL_SAMPLER("sampler_t"),
    OPENCL_EVENT("event_t"),
    OPENCL_CLK_EVENT("clk_event_t"),
    OPENCL_QUEUE("queue_t"),
    OPENCL_RESERVE_ID("reserve_id_t"),
    DEPENDENT,
    OVERLOAD,
    BOUND_MEMBER,
    PSEUDO_OBJECT,
    UNKNOWN_ANY("__builtin_any_type"),
    BUILTIN_FN,
    ARC_UNBRIDGED_CAST,
    OMP_ARRAY_SECTION;

    private static final Lazy<EnumHelper<BuiltinKind>> ENUM_HELPER = EnumHelper.newLazyHelper(BuiltinKind.class);

    public static EnumHelper<BuiltinKind> getEnumHelper() {
        return ENUM_HELPER.get();
    }

    private final String code;

    private BuiltinKind(String code) {
        this.code = code;
    }

    private BuiltinKind() {
        this.code = name().toLowerCase();
    }

    @Override
    public String getString() {
        return code;
    }

}
