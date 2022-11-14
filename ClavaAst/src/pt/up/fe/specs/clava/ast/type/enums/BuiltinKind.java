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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.extra.data.Language;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public enum BuiltinKind {
    OCLImage1dRO,
    OCLImage1dArrayRO,
    OCLImage1dBufferRO,
    OCLImage2dRO,
    OCLImage2dArrayRO,
    OCLImage2dDepthRO,
    OCLImage2dArrayDepthRO,
    OCLImage2dMSAARO,
    OCLImage2dArrayMSAARO,
    OCLImage2dMSAADepthRO,
    OCLImage2dArrayMSAADepthRO,
    OCLImage3dRO,
    OCLImage1dWO,
    OCLImage1dArrayWO,
    OCLImage1dBufferWO,
    OCLImage2dWO,
    OCLImage2dArrayWO,
    OCLImage2dDepthWO,
    OCLImage2dArrayDepthWO,
    OCLImage2dMSAAWO,
    OCLImage2dArrayMSAAWO,
    OCLImage2dMSAADepthWO,
    OCLImage2dArrayMSAADepthWO,
    OCLImage3dWO,
    OCLImage1dRW,
    OCLImage1dArrayRW,
    OCLImage1dBufferRW,
    OCLImage2dRW,
    OCLImage2dArrayRW,
    OCLImage2dDepthRW,
    OCLImage2dArrayDepthRW,
    OCLImage2dMSAARW,
    OCLImage2dArrayMSAARW,
    OCLImage2dMSAADepthRW,
    OCLImage2dArrayMSAADepthRW,
    OCLImage3dRW,
    OCLIntelSubgroupAVCMcePayload,
    OCLIntelSubgroupAVCImePayload,
    OCLIntelSubgroupAVCRefPayload,
    OCLIntelSubgroupAVCSicPayload,
    OCLIntelSubgroupAVCMceResult,
    OCLIntelSubgroupAVCImeResult,
    OCLIntelSubgroupAVCRefResult,
    OCLIntelSubgroupAVCSicResult,
    OCLIntelSubgroupAVCImeResultSingleRefStreamout,
    OCLIntelSubgroupAVCImeResultDualRefStreamout,
    OCLIntelSubgroupAVCImeSingleRefStreamin,
    OCLIntelSubgroupAVCImeDualRefStreamin,
    SveInt8,
    SveInt16,
    SveInt32,
    SveInt64,
    SveUint8,
    SveUint16,
    SveUint32,
    SveUint64,
    SveFloat16,
    SveFloat32,
    SveFloat64,
    SveBFloat16,
    SveInt8x2,
    SveInt16x2,
    SveInt32x2,
    SveInt64x2,
    SveUint8x2,
    SveUint16x2,
    SveUint32x2,
    SveUint64x2,
    SveFloat16x2,
    SveFloat32x2,
    SveFloat64x2,
    SveBFloat16x2,
    SveInt8x3,
    SveInt16x3,
    SveInt32x3,
    SveInt64x3,
    SveUint8x3,
    SveUint16x3,
    SveUint32x3,
    SveUint64x3,
    SveFloat16x3,
    SveFloat32x3,
    SveFloat64x3,
    SveBFloat16x3,
    SveInt8x4,
    SveInt16x4,
    SveInt32x4,
    SveInt64x4,
    SveUint8x4,
    SveUint16x4,
    SveUint32x4,
    SveUint64x4,
    SveFloat16x4,
    SveFloat32x4,
    SveFloat64x4,
    SveBFloat16x4,
    SveBool,
    VectorQuad,
    VectorPair,
    Void,
    Bool,
    Char_U,
    UChar,
    WChar_U,
    Char8,
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
    ShortAccum,
    Accum,
    LongAccum,
    UShortAccum,
    UAccum,
    ULongAccum,
    ShortFract,
    Fract,
    LongFract,
    UShortFract,
    UFract,
    ULongFract,
    SatShortAccum,
    SatAccum,
    SatLongAccum,
    SatUShortAccum,
    SatUAccum,
    SatULongAccum,
    SatShortFract,
    SatFract,
    SatLongFract,
    SatUShortFract,
    SatUFract,
    SatULongFract,
    Half,
    Float,
    Double,
    LongDouble,
    Float16,
    BFloat16,
    Float128,
    NullPtr,
    ObjCId,
    ObjCClass,
    ObjCSel,
    OCLSampler,
    OCLEvent,
    OCLClkEvent,
    OCLQueue,
    OCLReserveID,
    Dependent,
    Overload,
    BoundMember,
    PseudoObject,
    UnknownAny,
    BuiltinFn,
    ARCUnbridgedCast,
    IncompleteMatrixIdx,
    OMPArraySection,
    OMPArrayShaping,
    OMPIterator;

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
        // LITERAL_KINDS.put("ndrange_t", OCLNDRange);
        LITERAL_KINDS.put("reserve_id_t", OCLReserveID);
        LITERAL_KINDS.put("<dependent type>", Dependent);
        LITERAL_KINDS.put("<overloaded function type>", Overload);
        LITERAL_KINDS.put("<bound member function type>", BoundMember);
        LITERAL_KINDS.put("__builtin_any_type", UnknownAny);
        LITERAL_KINDS.put("<builtin fn type>", BuiltinFn);

    }

    /**
     * For built-in kinds that always have the same code.
     */
    private static final Map<BuiltinKind, String> BUILTIN_CODE;
    static {
        // Some of the built-in types:
        // https://android.googlesource.com/platform/prebuilts/clang/darwin-x86/sdk/3.5/+/refs/heads/master/include/clang/AST/BuiltinTypes.def

        BUILTIN_CODE = new HashMap<>();

        // Invert literal kinds
        for (var entry : LITERAL_KINDS.entrySet()) {
            // Skip bool and half, they have their own rules
            if (entry.getValue() == BuiltinKind.Bool || entry.getValue() == BuiltinKind.Half) {
                continue;
            }

            BUILTIN_CODE.put(entry.getValue(), entry.getKey());
        }

        // Manually add certain cases
        BUILTIN_CODE.put(Char_S, "char");

        /*
        BUILTIN_CODE.put(Void, "void");
        BUILTIN_CODE.put(Int, "int");
        BUILTIN_CODE.put(Long, "long");
        BUILTIN_CODE.put(LongLong, "long long");
        BUILTIN_CODE.put(Float, "float");
        BUILTIN_CODE.put(Double, "double");
        */
    }

    /**
     * Calculates the bit width of built-in kinds.
     */
    private static final Map<BuiltinKind, Function<Language, Integer>> BIT_WIDTHS;
    static {
        BIT_WIDTHS = new HashMap<>();
        BIT_WIDTHS.put(BuiltinKind.Double, lang -> lang.get(Language.DOUBLE_WIDTH));
        BIT_WIDTHS.put(BuiltinKind.LongDouble, lang -> lang.get(Language.LONG_DOUBLE_WIDTH));
        BIT_WIDTHS.put(BuiltinKind.Float, lang -> lang.get(Language.FLOAT_WIDTH));
        BIT_WIDTHS.put(BuiltinKind.Int, lang -> lang.get(Language.INT_WIDTH));
        BIT_WIDTHS.put(BuiltinKind.UInt, lang -> lang.get(Language.INT_WIDTH));
        BIT_WIDTHS.put(BuiltinKind.Long, lang -> lang.get(Language.LONG_WIDTH));
        BIT_WIDTHS.put(BuiltinKind.ULong, lang -> lang.get(Language.LONG_WIDTH));
        BIT_WIDTHS.put(BuiltinKind.LongLong, lang -> lang.get(Language.LONG_LONG_WIDTH));
        BIT_WIDTHS.put(BuiltinKind.ULongLong, lang -> lang.get(Language.LONG_LONG_WIDTH));
        BIT_WIDTHS.put(BuiltinKind.Short, lang -> lang.get(Language.SHORT_WIDTH));
        BIT_WIDTHS.put(BuiltinKind.UShort, lang -> lang.get(Language.SHORT_WIDTH));
        BIT_WIDTHS.put(BuiltinKind.Bool, lang -> lang.get(Language.BOOL_WIDTH));
        BIT_WIDTHS.put(BuiltinKind.SChar, lang -> lang.get(Language.CHAR_WIDTH));
        BIT_WIDTHS.put(BuiltinKind.UChar, lang -> lang.get(Language.CHAR_WIDTH));
        BIT_WIDTHS.put(BuiltinKind.Char_S, lang -> lang.get(Language.CHAR_WIDTH));
        BIT_WIDTHS.put(BuiltinKind.Char_U, lang -> lang.get(Language.CHAR_WIDTH));

        BIT_WIDTHS.put(BuiltinKind.Char8, lang -> 8);
        BIT_WIDTHS.put(BuiltinKind.Char16, lang -> 16);
        BIT_WIDTHS.put(BuiltinKind.Char32, lang -> 32);
        BIT_WIDTHS.put(BuiltinKind.UInt128, lang -> 128);
        BIT_WIDTHS.put(BuiltinKind.Int128, lang -> 128);
        BIT_WIDTHS.put(BuiltinKind.Float16, lang -> 16);
        BIT_WIDTHS.put(BuiltinKind.Float128, lang -> 128);

    }

    // public String getCode(ClavaContext context) {
    // return getCodeTry(context)
    // .orElseThrow(() -> new RuntimeException("Not implemented yet for kind '" + this + "'"));
    // }

    public Optional<String> getCodeTry(ClavaNode sourceNode) {
        return Optional.ofNullable(getCodePrivate(sourceNode));
    }

    private String getCodePrivate(ClavaNode sourceNode) {

        // Check if in fixed code table
        String code = BUILTIN_CODE.get(this);
        if (code != null) {
            return code;
        }

        // Special cases
        switch (this) {
        case Bool:
            return getCodeBool(sourceNode);
        default:
            // return null;
            throw new NotImplementedException(this);
        }
    }

    private String getCodeBool(ClavaNode sourceNode) {

        if (sourceNode == null) {
            return "bool";
        }

        // Get translation unit
        TranslationUnit tunit = sourceNode.getAncestorTry(TranslationUnit.class).orElse(null);
        if (tunit == null) {
            return "bool";
        }

        // If C++, just return bool
        if (tunit.get(TranslationUnit.LANGUAGE).get(Language.C_PLUS_PLUS)) {
            return "bool";
        }

        // C code, check if stdbool.h is an include
        if (tunit.hasInclude("stdbool.h", true)) {
            return "bool";
        }

        return "_Bool";
    }

    public static boolean isBuiltinKind(String literalKind) {
        return LITERAL_KINDS.containsKey(literalKind);
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

    public String getCode() {
        return getCodePrivate(null);
    }

    public int getBitwidth(Language lang) {
        return BIT_WIDTHS.getOrDefault(this, languange -> -1).apply(lang);
    }
}
