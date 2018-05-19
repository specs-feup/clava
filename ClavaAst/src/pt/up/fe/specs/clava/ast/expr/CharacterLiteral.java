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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.enums.CharacterKind;
import pt.up.fe.specs.util.SpecsStrings;

public class CharacterLiteral extends Literal {

    /// DATAKEYS BEGIN

    public final static DataKey<Long> VALUE = KeyFactory.longInt("value");
    public final static DataKey<CharacterKind> KIND = KeyFactory.enumeration("kind", CharacterKind.class);

    /// DATAKEYS END

    public CharacterLiteral(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /**
     * For legacy support.
     * 
     * @deprecated
     * @param charValue
     * @param exprData
     * @param info
     * @param children
     */
    @Deprecated
    protected CharacterLiteral(ExprData exprData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(exprData, info, children);
    }

    public long getCharValue() {
        return get(VALUE);
    }

    @Override
    public String getCode() {
        String sourceLiteral = get(SOURCE_LITERAL);

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
        String hexString = SpecsStrings.toHexString(get(VALUE), 8).substring("0x".length());
        return sourceLiteralPrefix + "'\\U" + hexString + "'";
    }

}
