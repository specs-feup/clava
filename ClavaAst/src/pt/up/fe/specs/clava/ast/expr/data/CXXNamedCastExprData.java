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

package pt.up.fe.specs.clava.ast.expr.data;

import pt.up.fe.specs.clava.language.CastKind;

public class CXXNamedCastExprData {

    private final String castName;
    private final String typeAsWritten;
    private final CastKind castKind;

    public CXXNamedCastExprData(String castName, String typeAsWritten, CastKind castKind) {
        this.castName = castName;
        this.typeAsWritten = typeAsWritten;
        this.castKind = castKind;
    }

    public String getCastName() {
        return castName;
    }

    public String getTypeAsWritten() {
        return typeAsWritten;
    }

    public CastKind getCastKind() {
        return castKind;
    }

}
