/**
 * Copyright 2022 SPeCS.
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

package pt.up.fe.specs.clava.utils;

import java.util.HashMap;
import java.util.Map;

public class CText {

    private static final Map<Byte, String> GENERAL_ESCAPE_SEQUENCES;
    static {
        GENERAL_ESCAPE_SEQUENCES = new HashMap<>();

        GENERAL_ESCAPE_SEQUENCES.put((byte) 0x5c, "\\\\");
        GENERAL_ESCAPE_SEQUENCES.put((byte) 0x07, "\\a");
        GENERAL_ESCAPE_SEQUENCES.put((byte) 0x08, "\\b");
        GENERAL_ESCAPE_SEQUENCES.put((byte) 0x0c, "\\f");
        GENERAL_ESCAPE_SEQUENCES.put((byte) 0x0a, "\\n");
        GENERAL_ESCAPE_SEQUENCES.put((byte) 0x0d, "\\r");
        GENERAL_ESCAPE_SEQUENCES.put((byte) 0x09, "\\t");
        GENERAL_ESCAPE_SEQUENCES.put((byte) 0x0b, "\\v");

    }

    private static final Map<Byte, String> CHAR_ESCAPE_SEQUENCES;
    static {
        CHAR_ESCAPE_SEQUENCES = new HashMap<>();

        CHAR_ESCAPE_SEQUENCES.put((byte) 0x27, "\\'");
        CHAR_ESCAPE_SEQUENCES.put((byte) 0x22, "\\\"");
        CHAR_ESCAPE_SEQUENCES.put((byte) 0x3f, "\\?");
    }

    private static final Map<Byte, String> STRING_ESCAPE_SEQUENCES;
    static {
        STRING_ESCAPE_SEQUENCES = new HashMap<>();

        STRING_ESCAPE_SEQUENCES.put((byte) 0x22, "\\\"");
    }

    /**
     * Transforms a byte representing an ASCII character into the equivalent C string representation.
     * 
     * <p>
     * Based on https://en.cppreference.com/w/cpp/language/escape
     * 
     * @param aByte
     * @return
     */
    public static String toCString(byte aByte, TextLiteralKind literalKind) {

        // Check if general escape sequence
        var escapeSequence = GENERAL_ESCAPE_SEQUENCES.get(aByte);

        if (escapeSequence != null) {
            return escapeSequence;
        }

        if (literalKind == TextLiteralKind.CHAR && CHAR_ESCAPE_SEQUENCES.containsKey(aByte)) {
            return CHAR_ESCAPE_SEQUENCES.get(aByte);
        }

        if (literalKind == TextLiteralKind.STRING && STRING_ESCAPE_SEQUENCES.containsKey(aByte)) {
            return STRING_ESCAPE_SEQUENCES.get(aByte);
        }

        // Check if byte can be safely converted to a character
        int intValue = aByte;

        if (intValue >= 32 && intValue <= 126) {
            var charArray = new char[1];
            charArray[0] = (char) intValue;
            return new String(charArray);
        }

        if (intValue < 256) {
            return "\\x" + String.format("%02X", (byte) intValue);
        }

        // Since it is accepting a byte, this should never execute
        throw new RuntimeException("Not implemented for values higher than 255. Current value: " + intValue);
    }

}
