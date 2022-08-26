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
import pt.up.fe.specs.clava.utils.CStrings;
import pt.up.fe.specs.clava.utils.TextLiteralKind;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.exceptions.CaseNotDefinedException;

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

    public final static DataKey<List<Byte>> STRING_BYTES = KeyFactory.generic("stringBytes",
            (List<Byte>) new ArrayList<Byte>());

    /// DATAKEY END

    public StringLiteral(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getLiteral() {
        // Update: Unfortunately it is not possible to blindly use the source code literal
        // For instance, if directives and macros appear in the middle of the literal, they will also appear in the
        // generated source code
        // Update End
        // Optimization: if not a raw string and is ASCII (most common case) can use stored literal
        // if (get(STRING_KIND) == StringKind.ASCII && !isRawStringLiteral()) {
        // return super.getLiteral();
        // }

        return get(STRING_KIND).getPrefix() + "\"" + getStringFromBytes() + "\"";
    }

    @Override
    public String getCode() {
        return getLiteral();
    }

    public boolean isRawStringLiteral() {
        return super.getLiteral().endsWith("R");
    }

    private String getStringFromBytes() {
        StringKind kind = get(STRING_KIND);

        // If UTF, just use String constructor from bytes
        if (kind.isUTF()) {
            List<Byte> byteList = get(STRING_BYTES);
            byte[] bytes = new byte[byteList.size()];
            for (int i = 0; i < byteList.size(); i++) {
                bytes[i] = byteList.get(i);
            }

            return SpecsStrings.escapeJson(new String(bytes, kind.getCharset()));
        }

        // If ASCII, convert each byte directly
        if (kind == StringKind.ASCII) {
            var literal = new StringBuilder();

            for (var aByte : get(STRING_BYTES)) {
                literal.append(CStrings.toCString(aByte, TextLiteralKind.STRING));
            }

            return literal.toString();
        }

        // If WIDE, take into account number of bytes per character
        if (kind == StringKind.WIDE) {

            // Build the string, escaping characters as necessary
            var chars = getBytesAsCharArray();

            var literal = new StringBuilder();

            for (var aChar : chars) {
                var asInt = (int) aChar;

                // If inside ASCII range, use ASCII mappings
                if (asInt < 256) {
                    literal.append(CStrings.toCString((byte) asInt, TextLiteralKind.STRING));
                    continue;
                }

                literal.append(aChar);
            }

            return literal.toString();
        }

        throw new CaseNotDefinedException(kind);
    }

    private char[] getBytesAsCharArray() {
        List<Byte> bytes = get(STRING_BYTES);
        int numBytesPerChar = get(CHAR_BYTE_WIDTH);
        char[] chars = new char[bytes.size() / numBytesPerChar];

        for (int i = 0; i < bytes.size(); i += numBytesPerChar) {

            char currentChar = getCharFromBytes(bytes, i, numBytesPerChar);
            chars[i / numBytesPerChar] = currentChar;
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

}
