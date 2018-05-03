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

import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum CharacterKind implements StringProvider {
    ASCII,
    WIDE,
    UTF8,
    UTF16,
    UTF32;

    private static final Lazy<EnumHelperWithValue<CharacterKind>> ENUM_HELPER = EnumHelperWithValue.newLazyHelper(CharacterKind.class);

    public static EnumHelperWithValue<CharacterKind> getEnumHelper() {
        return ENUM_HELPER.get();
    }

    private final String kind;

    private CharacterKind() {
        this.kind = name();
    }

    private CharacterKind(String kind) {
        this.kind = kind;
    }

    @Override
    public String getString() {
        return kind;
    }

}
