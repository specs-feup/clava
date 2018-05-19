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

import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

/**
 * 
 * @author JoaoBispo
 *
 */
public enum BuiltinTypeKeyword implements StringProvider {
    // List that can be used as base for keywords:
    // http://llvm-cs.pcc.me.uk/tools/clang/include/clang/AST/BuiltinTypes.def
    VOID,
    BOOL_C99("_Bool"),
    BOOL_CXX("bool"),
    CHAR,
    SIGNED,
    UNSIGNED,
    WCHAR_T,
    CHAR16_T,
    CHAR32_T,
    INT,
    SHORT,
    LONG,
    HALF("__fp16"),
    FLOAT,
    DOUBLE,
    LONG_DOUBLE("long double"),
    FLOAT16("_Float16"),
    FLOAT128("__float128"),
    INT128("__int128"),

    // OpenCL types
    SAMPLER("sampler_t"),
    EVENT("event_t"),
    CLK_EVENT("clk_event_t"),
    QUEUE("queue_t"),
    RESERVE_ID("reserve_id_t"),
    NULLPTR("nullptr_t");

    // private static final Lazy<EnumHelper<BuiltinTypeKeyword>> HELPER = EnumHelper
    // .newLazyHelper(BuiltinTypeKeyword.class);
    private static final Lazy<EnumHelperWithValue<BuiltinTypeKeyword>> HELPER = Lazy.newInstance(
            () -> new EnumHelperWithValue<>(BuiltinTypeKeyword.class).addAlias("half", HALF));

    public static EnumHelperWithValue<BuiltinTypeKeyword> getHelper() {
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