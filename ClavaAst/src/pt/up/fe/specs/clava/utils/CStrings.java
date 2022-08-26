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

public class CStrings {

    private static final Map<Byte, String> ESCAPE_SEQUENCES;
    static {
        ESCAPE_SEQUENCES = new HashMap<>();

        ESCAPE_SEQUENCES.put((byte) 0x27, "\\'");
        ESCAPE_SEQUENCES.put((byte) 0x22, "\\\"");
        ESCAPE_SEQUENCES.put((byte) 0x3f, "\\?");
        ESCAPE_SEQUENCES.put((byte) 0x5c, "\\\\");
        ESCAPE_SEQUENCES.put((byte) 0x07, "\\a");
        ESCAPE_SEQUENCES.put((byte) 0x08, "\\b");
        ESCAPE_SEQUENCES.put((byte) 0x0c, "\\f");
        ESCAPE_SEQUENCES.put((byte) 0x0a, "\\n");
        ESCAPE_SEQUENCES.put((byte) 0x0d, "\\r");
        ESCAPE_SEQUENCES.put((byte) 0x09, "\\t");
        ESCAPE_SEQUENCES.put((byte) 0x0b, "\\v");

        // if (currentChar == '\b') {
        // return "\\b";
        // }
        //
        // if (currentChar == '\f') {
        // return "\\f";
        // }
        //
        // if (currentChar == '\n') {
        // return "\\n";
        // }
        // if (currentChar == '\r') {
        // return "\\r";
        // }
        //
        // if (currentChar == '\t') {
        // escapedString.append("\\t");
        // continue;
        // }
        //
        // if (currentChar == '\"' || currentChar == '\\') {
        // escapedString.append("\\");
        // }

    }

    // public static String escapeString(String string) {
    //
    // }

    /**
     * Transforms a byte representing an ASCII character into the equivalent C string representation.
     * 
     * <p>
     * Based on https://en.cppreference.com/w/cpp/language/escape
     * 
     * @param aByte
     * @return
     */
    public static String toCString(byte aByte) {

        // Check if escape sequence
        var escapeSequence = ESCAPE_SEQUENCES.get(aByte);

        if (escapeSequence != null) {
            return escapeSequence;
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

        throw new RuntimeException("Not implemented for values higher than 255. Current value: " + intValue);

    }

}
