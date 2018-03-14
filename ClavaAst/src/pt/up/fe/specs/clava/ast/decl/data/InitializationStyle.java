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

package pt.up.fe.specs.clava.ast.decl.data;

import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum InitializationStyle implements StringProvider {
    NO_INIT,
    CINIT, // C-style initialization with assignment
    CALL_INIT("callinit"), // Call-style initialization (C++98)
    LIST_INIT("listinit"); // Direct list-initialization (C++11)

    private static Lazy<EnumHelper<InitializationStyle>> ENUM_HELPER = EnumHelper
            .newLazyHelper(InitializationStyle.class, NO_INIT);

    public static EnumHelper<InitializationStyle> getHelper() {
        return ENUM_HELPER.get();
    }

    private final String string;

    private InitializationStyle() {
        this.string = name().toLowerCase();
    }

    private InitializationStyle(String string) {
        this.string = string;
    }

    @Override
    public String getString() {
        return string;
    }
}