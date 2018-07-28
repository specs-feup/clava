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

package pt.up.fe.specs.clava.ast.type.data.exception;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.DataStore.ADataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.enums.ExceptionSpecificationType;

public class ExceptionSpecification extends ADataClass<ExceptionSpecification> {

    /// DATAKEYS BEGIN

    /**
     * The kind of exception specification.
     */
    public final static DataKey<ExceptionSpecificationType> EXCEPTION_SPECIFICATION_TYPE = KeyFactory
            .enumeration("exceptionSpecificationType", ExceptionSpecificationType.class);

    /**
     * Explicitly-specified list of exception types.
     */
    public final static DataKey<List<Type>> EXCEPTION_TYPES = KeyFactory
            .generic("exceptionTypes", (List<Type>) new ArrayList<Type>());

    /// DATAKEYS END

    public String getCode(FunctionProtoType type) {
        switch (get(EXCEPTION_SPECIFICATION_TYPE)) {
        case None:
            return "";
        case MSAny:
            return " throw(...)";
        case Dynamic:
            return "throw(" + get(EXCEPTION_TYPES).stream().map(Type::getCode).collect(Collectors.joining(", ")) + ")";
        case DynamicNone:
            return " throw()";
        case BasicNoexcept:
            return " noexcept";
        default:
            throw new RuntimeException("Not implemented yet for " + get(EXCEPTION_SPECIFICATION_TYPE));
        }
    }
}
