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
    UNDECLARED_IDENTIFIER(3797),
    UNDECLARED_IDENTIFIER_DID_YOU_MEAN(3798),
    NOT_STRUCT_OR_UNION(3748),
    NOT_A_FUNCTION_OR_FUNCTION_POINTER(3692),
    EXCESS_ELEMENTS_IN_INITIALIZER(2594),
    NO_MATCHING_FUNCTION(3363),
    TOO_MANY_ARGUMENTS(3699),
    NO_MEMBER(2997),
    NO_MEMBER_DID_YOU_MEAN(2999),
    CANT_INITIALIZE_WITH_TYPE(2773),
    INCOMPLETE_TYPE_STRUCT(2752),
    NO_MATCHING_CONSTRUCTOR(3364),
    DELEGATION_CYCLE(4833);


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