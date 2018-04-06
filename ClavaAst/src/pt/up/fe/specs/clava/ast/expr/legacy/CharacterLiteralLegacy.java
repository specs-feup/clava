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

package pt.up.fe.specs.clava.ast.expr.legacy;

import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.CharacterLiteral;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

public class CharacterLiteralLegacy extends CharacterLiteral {

    private final long charValue;

    public CharacterLiteralLegacy(long charValue, ExprData exprData, ClavaNodeInfo info) {
        this(charValue, exprData, info, Collections.emptyList());
    }

    private CharacterLiteralLegacy(long charValue, ExprData exprData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(exprData, info, children);

        this.charValue = charValue;
    }

    @Override
    public long getCharValue() {
        return charValue;
    }

    //
    // public CharacterLiteral(CharacterLiteralData data, List<? extends ClavaNode> children) {
    // super(null, null, children);
    // setData(data);
    // }

    @Override
    protected ClavaNode copyPrivate() {
        return new CharacterLiteralLegacy(charValue, getExprData(), getInfo(), Collections.emptyList());
    }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new CharacterLiteral(getData().copy(), Collections.emptyList());
    // // return new CharacterLiteral(charValue, getExprData(), getInfo(), Collections.emptyList());
    // }

    /*
    @Override
    public String getCode() {
        // long charValue = getData().getValue();
    
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
    */

    @Override
    public String toContentString() {
        return super.toContentString() + ", charValue:" + charValue;
    }

    /*
    @Override
    public String getLiteral() {
        return getCode();
    }
    */
    /*
    @Override
    public CharacterLiteralData getData() {
        return (CharacterLiteralData) super.getData();
    }
    */
}
