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

import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum PredefinedIdType implements StringProvider {
    Func,
    Function,
    LFunction, // Same as Function, but as wide string.
    FuncDName,
    FuncSig,
    LFuncSig, // Same as FuncSig, but as as wide string
    PrettyFunction,
    PrettyFunctionNoVirtual;

    private static final Lazy<EnumHelperWithValue<PredefinedIdType>> ENUM_HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(PredefinedIdType.class);

    public static EnumHelperWithValue<PredefinedIdType> getEnumHelper() {
        return ENUM_HELPER.get();
    }

    // private PredefinedIdType(String identTypeName) {
    // this.identTypeName = identTypeName;
    // }

    // private final String identTypeName;

    @Override
    public String getString() {
        switch (this) {
        case Func:
            return "__func__";
        case Function:
            return "__FUNCTION__";
        case LFunction:
            return "L__FUNCTION__";
        case FuncDName:
            return "__FUNCDNAME__";
        case FuncSig:
            return "__FUNCSIG__";
        case LFuncSig:
            return "L__FUNCSIG__";
        case PrettyFunction:
            return "__PRETTY_FUNCTION__";
        default:
            throw new NotImplementedException(this);
        }
        // return identTypeName;
    }

}