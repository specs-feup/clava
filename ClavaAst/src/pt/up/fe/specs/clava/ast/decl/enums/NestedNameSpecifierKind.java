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

package pt.up.fe.specs.clava.ast.decl.enums;

import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum NestedNameSpecifierKind implements StringProvider {
    Identifier,
    Namespace,
    NamespaceAlias,
    TypeSpec,
    TypeSpecWithTemplate,
    Global,
    Super;
    // NONE("");

    private static final Lazy<EnumHelperWithValue<NestedNameSpecifierKind>> ENUM_HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(NestedNameSpecifierKind.class);

    public static EnumHelperWithValue<NestedNameSpecifierKind> getEnumHelper() {
        return ENUM_HELPER.get();
    }

    // private final String description;
    //
    // private NestedNamedSpecifier(String description) {
    // this.description = description;
    // }

    @Override
    public String getString() {
        return name();
        // return description;
    }
}