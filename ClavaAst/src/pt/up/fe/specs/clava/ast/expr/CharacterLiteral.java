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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.SpecsStrings;

public class CharacterLiteral extends Literal {

    private static final Map<Character, String> CHAR_LITERAL;
    static {
        CHAR_LITERAL = new HashMap<>();
        CHAR_LITERAL.put('\n', "\\n");
        CHAR_LITERAL.put('\t', "\\t");
        CHAR_LITERAL.put('\u000b', "\\v");
        CHAR_LITERAL.put('\b', "\\b");
        CHAR_LITERAL.put('\r', "\\r");
        CHAR_LITERAL.put('\f', "\\f");
        CHAR_LITERAL.put('\u0007', "\\a");
        CHAR_LITERAL.put('\\', "\\\\");
        CHAR_LITERAL.put((char) 63, "\\?");
        CHAR_LITERAL.put('\'', "\\'");
        CHAR_LITERAL.put('"', "\\\"");
        CHAR_LITERAL.put('\0', "\\0");
    }

    private final long charValue;

    public CharacterLiteral(long charValue, ExprData exprData, ClavaNodeInfo info) {
        this(charValue, exprData, info, Collections.emptyList());
    }

    private CharacterLiteral(long charValue, ExprData exprData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(exprData, info, children);

        this.charValue = charValue;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CharacterLiteral(charValue, getExprData(), getInfo(), Collections.emptyList());
    }

    @Override
    public String getCode() {
        // Check if value is inside Character boundaries
        if (charValue > (long) Character.MAX_VALUE) {
            throw new RuntimeException("Not implemented dealing with values larger than Character.MAX_VALUE");
        }

        char aChar = (char) charValue;

        // Check if character is inside table
        String literal = CHAR_LITERAL.get(aChar);
        if (literal != null) {
            return "'" + literal + "'";
        }

        // ASCII characters
        if (aChar < 128) {
            if (SpecsStrings.isPrintableChar(aChar)) {
                return "'" + aChar + "'";
            }

            // Not printable, just return as an int
            return Integer.toString(aChar);
        }

        // Print as Unicode
        String hexString = SpecsStrings.toHexString(aChar, 8).substring("0x".length());
        return "u'\\U" + hexString + "'";
    }

    @Override
    public String toContentString() {
        return super.toContentString() + ", charValue:" + charValue;
    }

    @Override
    public String getLiteral() {
        return getCode();
    }
}
