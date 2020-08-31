/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.tupatcher;

import java.util.HashMap;
import java.util.Map;

public enum ErrorKind {
    
    UNKNOWN_TYPE(3822),
    UNKNOWN_TYPE_DID_YOU_MEAN(3823),
    INCOMPATIBLE_TYPE(3717),
    INCOMPATIBLE_OPERAND_TYPES(3715),
    REFERENCE_TO_TYPE_COULD_NOT_BIND_TO_RVALUE(3474),
    COMPARISON_BETWEEN(3711),
    COMPARISON_OF_DISTINCT_POINTER_TYPES(3709),
    NO_VIABLE_CONVERSION(3755),
    UNDECLARED_IDENTIFIER(3797),
    UNDECLARED_IDENTIFIER_DID_YOU_MEAN(3798),
    NOT_STRUCT_OR_UNION(3748),
    CANT_BE_REFERENCED_WITH_STRUCT(3573),
    NOT_CLASS_NAMESPACE_OR_ENUMERATION(2596),
    REDEFINITION_WITH_DIFFERENT_TYPE(3457),
    NOT_A_FUNCTION_OR_FUNCTION_POINTER(3692),
    EXCESS_ELEMENTS_IN_INITIALIZER(2594),
    NO_MATCHING_FUNCTION(3363),
    TOO_MANY_ARGUMENTS(3699),
    NO_MEMBER(2997),
    NO_MEMBER_DID_YOU_MEAN(2999),
    CANT_INITIALIZE_WITH_TYPE(2773),
    INCOMPLETE_TYPE_STRUCT(2752),
    TYPE_IS_NOT_POINTER(3745),
    INDIRECTION_REQUIRES_POINTER_OPERAND(3735),
    VARIABLE_INCOMPLETE_TYPE_STRUCT(3722),
    NO_MATCHING_CONSTRUCTOR(3364),
    DELEGATION_CYCLE(4833),
    INVALID_USE_OF_NON_STATIC(2822),
    NON_STATIC_MEMBER_FUNCTION(2901),
    NON_CONST_CANT_BIND_TO_TEMPORARY(2885),
    BASE_OF_MEMBER_REFERENCE_IS_A_FUNCTION(2916),
    INVALID_OPERANDS_TO_BINARY_EXPRESSION(3738),
    NO_VIABLE_OVERLOADED(3368),
    IS_NOT_ARRAY_POINTER_OR_VECTOR(3767),
    NO_TYPE_NAMED_IN(3779),
    FUNCTION_IS_NOT_MARKED_CONST(2907),
    CASE_VALUE_IS_NOT_CONST(2624),
    CXX_REQUIRES_TYPE_SPECIFIER(2939);


    private static final Map<Integer, ErrorKind> KIND_MAP = new HashMap<>();
    static {
        for (var kind : ErrorKind.values()) {
            KIND_MAP.put(kind.getId(), kind);
        }
    }

    public static ErrorKind getKind(int errorNumber) {
        return KIND_MAP.get(errorNumber);
    }

    private final int id;

    private ErrorKind(int code) {
        this.id = code;
    }

    public int getId() {
        return id;
    }



}
