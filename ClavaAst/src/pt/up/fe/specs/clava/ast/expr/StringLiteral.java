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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.enums.StringKind;
import pt.up.fe.specs.util.SpecsStrings;

public class StringLiteral extends Literal {

    /// DATAKEY BEGIN

    public final static DataKey<StringKind> STRING_KIND = KeyFactory.enumeration("stringKind", StringKind.class);

    /**
     * Number of characters of the string.
     */
    public final static DataKey<Long> LENGTH = KeyFactory.longInt("length");

    /**
     * Number of bytes per character.
     */
    public final static DataKey<Integer> CHAR_BYTE_WIDTH = KeyFactory.integer("charByteWidth");

    // public final static DataKey<String> STRING = KeyFactory.string("string");

    public final static DataKey<List<Byte>> STRING_BYTES = KeyFactory.generic("stringBytes",
            (List<Byte>) new ArrayList<Byte>());

    /// DATAKEY END

    // private final String string;

    public StringLiteral(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public StringLiteral(String string, ExprData exprData, ClavaNodeInfo info) {
    // // According to CPP reference, string literals are lvalues
    // // http://en.cppreference.com/w/cpp/language/value_category
    // super(exprData, info, Collections.emptyList());
    //
    // this.string = string;
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new StringLiteral(string, getExprData(), getInfo());
    // }

    // public String getString() {
    // return getLiteral();
    // // return string;
    // }

    // public String getStringContents() {
    // // return get(STRING);
    // String literalString = getString();
    // return literalString.substring(1, literalString.length() - 1);
    // String stringContents = string;
    // if (stringContents.startsWith("\"")) {
    // stringContents = stringContents.substring(1);
    // }
    // if (stringContents.endsWith("\"")) {
    //
    // }
    //
    // return string;
    // }

    @Override
    public String getLiteral() {
        // Optimization: if not a raw string and is ASCII (most common case) can use stored literal
        if (get(STRING_KIND) == StringKind.ASCII && !isRawStringLiteral()) {
            return super.getLiteral();
        }

        return get(STRING_KIND).getPrefix() + "\"" + SpecsStrings.escapeJson(getStringFromBytes()) + "\"";
        // // If raw string literal, build the string from the bytes
        // if (isRawStringLiteral()) {
        // return get(STRING_KIND).getPrefix() + "\"" + SpecsStrings.escapeJson(getStringFromBytes()) + "\"";
        // }
        //
        // return super.getLiteral();
    }

    @Override
    public String getCode() {
        // System.out.println("KIND:" + get(STRING_KIND));
        // System.out.println("BYTES:" + get(STRING_BYTES));
        // System.out.println("SUPER LITERAL:" + super.getLiteral());
        // System.out.println("LITERAL:" + getLiteral());
        // System.out.println("IS RAW STRING:" + isRawStringLiteral());

        // System.out.println("String from BYTES:" + SpecsStrings.escapeJson(getStringFromBytes()));
        return getLiteral();
        /*
        // String string = getEscapedString();
        
        // if (string == null) {
        // return getLiteral();
        // }
        
        // Build the string from the bytes
        // System.out.println("STRING LITRAL:" + getString());
        // return getLiteral();
        StringBuilder code = new StringBuilder();
        
        // code.append(get(STRING_KIND).getPrefix());
        // code.append("\"");
        // code.append(string);
        code.append(getLiteral());
        // code.append("\"");
        
        return code.toString();
        // return "\"" + getString() + "\"";
        // System.out.println("STRING CONTENTS:" + getStringContents());
        // System.out.println("LITERAL:" + getLiteral());
        // System.out.println("ESCAPED:" + SpecsStrings.escapeJson(getStringContents()));
        // // return getString();
        // return "\"" + SpecsStrings.escapeJson(getStringContents()) + "\"";
         *
         */
    }

    public boolean isRawStringLiteral() {
        return super.getLiteral().endsWith("R");
    }

    /**
     * The string, built from the original bytes.
     * 
     * <p>
     * TODO: UTF prefixes not supported yet, will return null for those cases. Use getLiteral() instead.
     * 
     * @return
     */
    /*
    public String getEscapedString() {
        String unescapedString = SpecsStrings.escapeJson(getStringFromBytes());
        if (unescapedString == null) {
            return null;
        }
    
        return unescapedString;
    }
    */

    private String getStringFromBytes() {
        StringKind kind = get(STRING_KIND);

        // If UTF, just use String constructor from bytes
        if (kind.isUTF()) {
            List<Byte> byteList = get(STRING_BYTES);
            byte[] bytes = new byte[byteList.size()];
            for (int i = 0; i < byteList.size(); i++) {
                bytes[i] = byteList.get(i);
            }

            return new String(bytes, kind.getCharset());
        }

        return new String(getBytesAsCharArray());
        //
        // switch (get(STRING_KIND)) {
        // case ASCII:
        // return new String(getBytesAsChars());
        // case WIDE:
        // return new String(getBytesAsWideChars());
        // default:
        // // ClavaLog.debug("String literals of kind '" + get(STRING_KIND)
        // // + "' not properly implemented yet, using Clang literal");
        // // return null;
        // throw new RuntimeException(
        // "String literals from bytes of kind '" + get(STRING_KIND) + "' not implemented yet. Node: " + this);
        // }
    }

    private char[] getBytesAsCharArray() {
        List<Byte> bytes = get(STRING_BYTES);
        int numBytesPerChar = get(CHAR_BYTE_WIDTH);
        char[] chars = new char[bytes.size() / numBytesPerChar];

        for (int i = 0; i < bytes.size(); i += numBytesPerChar) {

            char currentChar = getCharFromBytes(bytes, i, numBytesPerChar);
            chars[i / numBytesPerChar] = currentChar;

            // int mask1 = 0xFF;
            // int mask2 = 0xFF << 8;
            //
            // int lowerByte = bytes.get(i).intValue() & mask1;
            // int higherByte = (bytes.get(i + 1).intValue() << 8) & mask2;
            //
            // // char[] newChars = Character.toChars(higherByte | lowerByte);
            // // Preconditions.checkArgument(newChars.length == 1, "Expected length to be 1");
            // // chars[i / 2] = newChars[0];
            // System.out.println("HIGHER BYTE:" + Integer.toHexString(higherByte));
            // System.out.println("LOWER BYTE:" + Integer.toHexString(lowerByte));
            // System.out.println("FULL INT: " + Integer.toHexString((higherByte | lowerByte)));
            // chars[i / numBytesPerChar] = (char) (higherByte | lowerByte);
            // // for (char aChar : newChars) {
            // // chars.add(aChar);
            // // }
            //
            // // chars[i / 2] = (char) (higherByte | lowerByte);
        }

        return chars;
    }

    private char getCharFromBytes(List<Byte> bytes, int startIndex, int numBytesPerChar) {
        int baseMask = 0xFF;
        int currentChar = 0;

        for (int i = 0; i < numBytesPerChar; i++) {
            // int adjustedMask = baseMask << (8 * i);
            int currentByte = bytes.get(startIndex + i) & baseMask;
            currentChar = currentChar | (currentByte << (8 * i));
        }

        return (char) currentChar;
    }

    // private char[] getBytesAsChars() {
    //
    // List<Byte> bytes = get(STRING_BYTES);
    // char[] chars = new char[bytes.size()];
    //
    // for (int i = 0; i < bytes.size(); i++) {
    // chars[i] = (char) bytes.get(i).intValue();
    // }
    //
    // return chars;
    // }
    //
    // private char[] getBytesAsWideChars() {
    // // private List<Character> getBytesAsWideChars() {
    // List<Byte> bytes = get(STRING_BYTES);
    // char[] chars = new char[bytes.size() / 2];
    //
    // // List<Character> chars = new ArrayList<>();
    //
    // for (int i = 0; i < bytes.size(); i += 2) {
    //
    // int mask1 = 0xFF;
    // int mask2 = 0xFF << 8;
    //
    // int lowerByte = bytes.get(i).intValue() & mask1;
    // int higherByte = (bytes.get(i + 1).intValue() << 8) & mask2;
    //
    // // char[] newChars = Character.toChars(higherByte | lowerByte);
    // // Preconditions.checkArgument(newChars.length == 1, "Expected length to be 1");
    // // chars[i / 2] = newChars[0];
    // System.out.println("HIGHER BYTE:" + Integer.toHexString(higherByte));
    // System.out.println("LOWER BYTE:" + Integer.toHexString(lowerByte));
    // System.out.println("FULL INT: " + Integer.toHexString((higherByte | lowerByte)));
    // chars[i / 2] = (char) (higherByte | lowerByte);
    // // for (char aChar : newChars) {
    // // chars.add(aChar);
    // // }
    //
    // // chars[i / 2] = (char) (higherByte | lowerByte);
    // }
    //
    // return chars;
    // }

    // @Override
    // public String toContentString() {
    // return ClavaNode.toContentString(super.toContentString(), "string:" + string);
    // // return super.toContentString() + "string:" + string;
    // }

    // @Override
    // public String getLiteral() {
    // return string;
    // }

    // StringKind getKind()

}
