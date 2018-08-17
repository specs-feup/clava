/**
 *  Copyright 2018 SPeCS.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package pt.up.fe.specs.clava.ast.expr.enums;

import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;

public enum FloatKind {
    DOUBLE(BuiltinKind.Double),
    FLOAT(BuiltinKind.Long),
    LONG_DOUBLE(BuiltinKind.LongDouble);

    private final BuiltinKind builtinKind;

    private FloatKind(BuiltinKind builtinKind) {
        this.builtinKind = builtinKind;
    }

    public BuiltinKind getBuiltinKind() {
        return builtinKind;
    }
}