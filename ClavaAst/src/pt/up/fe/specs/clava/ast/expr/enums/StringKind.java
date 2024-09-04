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

package pt.up.fe.specs.clava.ast.expr.enums;

import java.nio.charset.Charset;

import pt.up.fe.specs.util.exceptions.NotImplementedException;

public enum StringKind {

    ORDINARY,
    WIDE,
    UTF8(true),
    UTF16(true),
    UTF32(true);

    private final boolean isUTF;

    private StringKind(boolean isUtf) {
        this.isUTF = isUtf;
    }

    private StringKind() {
        this(false);
    }

    public String getPrefix() {
        switch (this) {
        case ORDINARY:
            return "";
        case WIDE:
            return "L";
        case UTF8:
            return "u8";
        case UTF16:
            return "u";
        case UTF32:
            return "U";
        default:
            throw new NotImplementedException(this);
        }

    }

    public boolean isUTF() {
        return isUTF;
    }

    public Charset getCharset() {
        switch (this) {
        case UTF8:
            return Charset.forName("UTF-8");
        case UTF16:
            return Charset.forName("UTF-16BE");
        case UTF32:
            return Charset.forName("UTF-32BE");
        default:
            throw new RuntimeException("No charset defined for string kind '" + this + "'");

        }
    }
}
