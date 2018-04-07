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

package pt.up.fe.specs.clava.ast.type.data2;

import pt.up.fe.specs.clava.ast.expr.enums.BuiltinKind;

public class BuiltinTypeData extends TypeDataV2 {

    private final BuiltinKind kind;
    private final boolean isSugared;

    public BuiltinTypeData(BuiltinKind kind, boolean isSugared, TypeDataV2 data) {
        super(data);

        this.kind = kind;
        this.isSugared = isSugared;
    }

    public BuiltinTypeData(BuiltinTypeData data) {
        this(data.kind, data.isSugared, data);
    }

    @Override
    public String toString() {
        return toString(super.toString(), "kind: " + kind + ", isSugared: " + isSugared);
    }

    public BuiltinKind getKind() {
        return kind;
    }

    public boolean isSugared() {
        return isSugared;
    }
}
