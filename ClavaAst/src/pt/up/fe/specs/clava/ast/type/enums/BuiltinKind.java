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

import java.util.LinkedHashMap;
import java.util.Map;

import pt.up.fe.specs.clava.context.ClavaContext;

public enum BuiltinKind {
    Void,
    Bool,
    Char_U,
    UChar,
    WChar_U,
    Char16,
    Char32,
    UShort,
    UInt,
    ULong,
    ULongLong,
    UInt128,
    Char_S,
    SChar,
    WChar_S,
    Short,
    Int,
    Long,
    LongLong,
    Int128,
    Half,
    Float,
    Double,
    LongDouble,
    NullPtr,
    ObjCId,
    ObjCClass,
    ObjCSel,
    OCLImage1d,
    OCLImage1dArray,
    OCLImage1dBuffer,
    OCLImage2d,
    OCLImage2dArray,
    OCLImage2dDepth,
    OCLImage2dArrayDepth,
    OCLImage2dMSAA,
    OCLImage2dArrayMSAA,
    OCLImage2dMSAADepth,
    OCLImage2dArrayMSAADepth,
    OCLImage3d,
    OCLSampler,
    OCLEvent,
    OCLClkEvent,
    OCLQueue,
    OCLNDRange,
    OCLReserveID,
    Dependent,
    Overload,
    BoundMember,
    PseudoObject,
    UnknownAny,
    BuiltinFn;

    private static final Map<String, BuiltinKind> LITERAL_KINDS;
    static {
        LITERAL_KINDS = new LinkedHashMap<>();
        LITERAL_KINDS.put("void", Void);
        LITERAL_KINDS.put("bool", Bool);
        LITERAL_KINDS.put("_Bool", Bool);
        // LITERAL_KINDS.put("char", Char_U);
        LITERAL_KINDS.put("unsigned char", UChar);
        LITERAL_KINDS.put("wchar_t", WChar_U);
        LITERAL_KINDS.put("char16_t", Char16);
        LITERAL_KINDS.put("char32_t", Char32);
        LITERAL_KINDS.put("unsigned short", UShort);
        LITERAL_KINDS.put("unsigned int", UInt);
        LITERAL_KINDS.put("unsigned long", ULong);
        LITERAL_KINDS.put("unsigned long long", ULongLong);
        LITERAL_KINDS.put("unsigned __int128", UInt128);
        // LITERAL_KINDS.put("char", Char_S);
        LITERAL_KINDS.put("signed char", SChar);
        LITERAL_KINDS.put("wchar_t", WChar_S);
        LITERAL_KINDS.put("short", Short);
        LITERAL_KINDS.put("int", Int);
        LITERAL_KINDS.put("long", Long);
        LITERAL_KINDS.put("long long", LongLong);
        LITERAL_KINDS.put("__int128_t", Int128);
        LITERAL_KINDS.put("half", Half);
        LITERAL_KINDS.put("__fp16", Half);
        LITERAL_KINDS.put("float", Float);
        LITERAL_KINDS.put("double", Double);
        LITERAL_KINDS.put("long double", LongDouble);
        LITERAL_KINDS.put("nullptr_t", NullPtr);
        LITERAL_KINDS.put("id", ObjCId);
        LITERAL_KINDS.put("Class", ObjCClass);
        LITERAL_KINDS.put("SEL", ObjCSel);
        LITERAL_KINDS.put("sampler_t", OCLSampler);
        LITERAL_KINDS.put("event_t", OCLEvent);
        LITERAL_KINDS.put("clk_event_t", OCLClkEvent);
        LITERAL_KINDS.put("queue_t", OCLQueue);
        LITERAL_KINDS.put("ndrange_t", OCLNDRange);
        LITERAL_KINDS.put("reserve_id_t", OCLReserveID);
        LITERAL_KINDS.put("<dependent type>", Dependent);
        LITERAL_KINDS.put("<overloaded function type>", Overload);
        LITERAL_KINDS.put("<bound member function type>", BoundMember);
        LITERAL_KINDS.put("__builtin_any_type", UnknownAny);
        LITERAL_KINDS.put("<builtin fn type>", BuiltinFn);

    }

    public String getCode(ClavaContext context) {

        switch (this) {
        case Void:
            return "void";
        case Int:
            return "int";
        default:
            throw new RuntimeException("Case not defined:" + this);
        }
    }

    public static BuiltinKind newInstance(String literalKind) {
        BuiltinKind builtinKind = LITERAL_KINDS.get(literalKind);
        if (builtinKind == null) {
            throw new RuntimeException("Literal '" + literalKind
                    + "' not supported as a BuiltinKind. Supported literals: " + LITERAL_KINDS.keySet());
        }

        return builtinKind;
    }

    public boolean isInteger() {
        return this.ordinal() >= Bool.ordinal() && this.ordinal() <= Int128.ordinal();
    }

    public boolean isSignedInteger() {
        return this.ordinal() >= Char_S.ordinal() && this.ordinal() <= Int128.ordinal();
    }

    public boolean isUnsignedInteger() {
        return this.ordinal() >= Bool.ordinal() && this.ordinal() <= UInt128.ordinal();
    }

    public boolean isFloatingPoint() {
        return this.ordinal() >= Half.ordinal() && this.ordinal() <= LongDouble.ordinal();
    }
}
