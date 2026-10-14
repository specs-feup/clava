/**
 * Copyright 2016 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.enums.CharacterKind;
import pt.up.fe.specs.util.SpecsStrings;

public class CharacterLiteral extends CharacterLiteralBase {

    // Data declarations are generated from the wire schema in CharacterLiteralBase.

    public CharacterLiteral(DataStore data, Collection<? extends ClavaNode> children, CharacterLiteralBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public CharacterLiteral(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public long getCharValue() {
        return getWireValue();
    }

    @Override
    public String getCode() {
        String sourceLiteral = getWireSourceLiteral();

        // If source literal starts with ', then just return
        if (sourceLiteral.startsWith("'")) {
            return sourceLiteral;
        }

        if (sourceLiteral.equals("u") || sourceLiteral.equals("U")) {
            return getCodeFromUnicode(sourceLiteral);
        }

        throw new RuntimeException("Case not supported for source literal '" + sourceLiteral + "'");
    }

    private String getCodeFromUnicode(String sourceLiteralPrefix) {
        String hexString = SpecsStrings.toHexString(getWireValue(), 8).substring("0x".length());
        return sourceLiteralPrefix + "'\\U" + hexString + "'";
    }

}
