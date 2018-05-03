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

public enum NestedNamedSpecifier implements StringProvider {
    IDENTIFIER("Identifier"),
    NAMESPACE("Namespace"),
    NAMESPACE_ALIAS("NamespaceAlias"),
    TYPE_SPEC("TypeSpec"),
    TYPE_SPEC_WITH_TEMPLATE("TypeSpecWithTemplate"),
    GLOBAL("Global"),
    SUPER("Super"),
    NONE("");

    private static final Lazy<EnumHelperWithValue<NestedNamedSpecifier>> ENUM_HELPER = EnumHelperWithValue
            .newLazyHelper(NestedNamedSpecifier.class, NONE);

    public static EnumHelperWithValue<NestedNamedSpecifier> getEnumHelper() {
        return ENUM_HELPER.get();
    }

    private final String description;

    private NestedNamedSpecifier(String description) {
        this.description = description;
    }

    @Override
    public String getString() {
        return description;
    }
}