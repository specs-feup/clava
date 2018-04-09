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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum BuiltinKind implements StringProvider {

    VOID,

    // Unsigned Types
    C_BOOL("bool"),
    CXX_BOOL("_Bool"),
    CHAR,
    UNSIGNED_CHAR("unsigned char"),
    WCHAR_T("wchar_t"),
    CHAR16_T("char16_t"),
    CHAR32_T("char32_t"),
    UNSIGNED_SHORT("unsigned short"),
    UNSIGNED_INT("unsigned int"),
    UNSIGNED_LONG("unsigned long"),
    UNSIGNED_LONG_LONG("unsigned long long"),
    UINT128_T("unsigned __int128"),

    // Signed Types
    CHAR_SIGNED("char"),
    CHAR_SIGNED_EXPLICIT("signed char"),
    WCHAR_T_SIGNED("wchar_t"),
    SHORT("short"),
    INT("int"),
    LONG("long"),
    LONG_LONG("long long"),
    INT128_T("__int128_t"),

    // Floating point types
    HALF("__fp16"),
    FLOAT("float"),
    DOUBLE("double"),
    LONG_DOUBLE("long double"),

    // Language-specific types
    NULLPTR("nullptr_t"),
    OBJ_C_ID("id"),
    OBJ_C_CLASS("Class"),
    OBJ_C_SEL("SEL"),

    // OpenCL image types
    OPENCL_IMAGE_1D,
    OPENCL_IMAGE_1D_ARRAY,
    OPENCL_IMAGE_1D_BUFFER,
    OPENCL_IMAGE_2D,
    OPENCL_IMAGE_2D_ARRAY,
    OPENCL_IMAGE_2D_DEPTH,
    OPENCL_IMAGE_2D_ARRAY_DEPTH,
    OPENCL_IMAGE_2D_MSAA,
    OPENCL_IMAGE_2D_ARRAY_MSAA,
    OPENCL_IMAGE_2D_MSAA_DEPTH,
    OPENCL_IMAGE_2D_ARRAY_MSAA_DEPTH,
    OPENCL_IMAGE_3D,

    // OpenCL types
    OPENCL_SAMPLER("sampler_t"),
    OPENCL_EVENT("event_t"),
    OPENCL_CLK_EVENT("clk_event_t"),
    OPENCL_QUEUE("queue_t"),
    OPENCL_NDRANGE("ndrange_t"),
    OPENCL_RESERVE_ID("reserve_id_t"),

    DEPENDENT("<dependent type>"),
    OVERLOAD,
    BOUND_MEMBER("<bound member function type>"),
    PSEUDO_OBJECT,
    UNKNOWN_ANY("__builtin_any_type"),
    BUILTIN_FN,
    ARC_UNBRIDGED_CAST,
    OMP_ARRAY_SECTION;

    private static final Set<BuiltinKind> UNSIGNED_TYPES = new HashSet<>(Arrays.asList(
            C_BOOL,
            CXX_BOOL,
            CHAR,
            UNSIGNED_CHAR,
            WCHAR_T,
            CHAR16_T,
            CHAR32_T,
            UNSIGNED_SHORT,
            UNSIGNED_INT,
            UNSIGNED_LONG,
            UNSIGNED_LONG_LONG,
            UINT128_T));
    // Clang 6.0
    /*
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
    */
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

    public boolean isUnsigned() {
        return UNSIGNED_TYPES.contains(this);
    }

}
