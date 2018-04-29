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

package pt.up.fe.specs.clava.ast.expr.data2;

import pt.up.fe.specs.clava.ast.expr.enums.CharacterKind;

/**
 * @deprecated
 * @author JoaoBispo
 *
 */
@Deprecated
public class CharacterLiteralData extends LiteralData {

    private final long value;
    private final CharacterKind kind;

    public CharacterLiteralData(long value, CharacterKind kind, LiteralData data) {
        super(data);

        this.value = value;
        this.kind = kind;
    }

    public CharacterLiteralData(CharacterLiteralData data) {
        this(data.value, data.kind, data);
    }

    @Override
    public String toString() {
        return toString(super.toString(), "charValue: " + value + ", kind: " + kind);
    }

    public long getValue() {
        return value;
    }

    public CharacterKind getKind() {
        return kind;
    }

}
