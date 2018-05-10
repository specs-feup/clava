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
import java.util.Map;

import pt.up.fe.specs.clava.context.ClavaContext;

public enum BuiltinKindV2 {
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
        LITERAL_KINDS = new HashMap<>();
    }

    public String getCode(ClavaContext context) {

        switch (this) {
        case Void:
            return "void";
        default:
            throw new RuntimeException("Case not defined:" + this);
        }
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
