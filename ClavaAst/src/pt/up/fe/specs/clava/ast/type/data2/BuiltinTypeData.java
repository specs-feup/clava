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

import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.clava.ast.expr.enums.BuiltinKind;

/**
 * @deprecated
 * @author JoaoBispo
 *
 */
@Deprecated
public class BuiltinTypeData extends TypeDataV2 {

    private final int kindOrdinal;
    private final BuiltinKind kind;
    private final boolean isSugared;

    public BuiltinTypeData(int kindOrdinal, BuiltinKind kind, boolean isSugared, TypeDataV2 data) {
        super(data);

        this.kindOrdinal = kindOrdinal;
        this.kind = kind;
        this.isSugared = isSugared;

        // System.out.println("BUILTIN TYPE:" + toString());
    }

    /**
     *
     * TODO:TEMPORARY
     * 
     * @param kind
     * @param isSugared
     * @param data
     */
    public BuiltinTypeData(BuiltinKind kind, boolean isSugared, TypeDataV2 data) {
        this(-1, kind, isSugared, data);
    }

    public BuiltinTypeData(BuiltinTypeData data) {
        // this(data.kind, data.isSugared, data.standard, data);
        this(data.kindOrdinal, data.kind, data.isSugared, data);
    }

    /**
     * @deprecated Instead implement .empty(), and use set
     * @param builtinKind
     */
    @Deprecated
    public BuiltinTypeData(BuiltinKind builtinKind) {
        this(-1, builtinKind, false, TypeDataV2.empty(new ClavaData()));
    }

    // public BuiltinTypeData(String builtinKind) {
    // this(BuiltinKind.getHelper().valueOf(builtinKind));
    // }

    @Override
    public String toString() {
        return toString(super.toString(),
                "kindOrdinal: " + kindOrdinal + ", kind: " + kind + ", isSugared: " + isSugared);
    }

    public BuiltinKind getKind() {
        return kind;
    }

    public boolean isSugared() {
        return isSugared;
    }

    // public Standard getStandard() {
    // return standard;
    // }
}
