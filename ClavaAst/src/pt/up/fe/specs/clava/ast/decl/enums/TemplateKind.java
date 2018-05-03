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

package pt.up.fe.specs.clava.ast.decl.enums;

import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum TemplateKind implements StringProvider {
    NON_TEMPLATE,
    FUNCTION_TEMPLATE,
    MEMBER_SPECIALIZATION,
    FUNCTION_TEMPLATE_SPECIALIZATION,
    DEPENDENT_FUNCTION_TEMPLATE_SPECIALIZATION;

    private static final Lazy<EnumHelperWithValue<TemplateKind>> ENUM_HELPER = EnumHelperWithValue.newLazyHelperWithValue(TemplateKind.class);

    public static EnumHelperWithValue<TemplateKind> getHelper() {
        return ENUM_HELPER.get();
    }

    @Override
    public String getString() {
        return name();
    }

    public boolean isSpecialization() {
        if (this == TemplateKind.NON_TEMPLATE || this == TemplateKind.FUNCTION_TEMPLATE) {
            return false;
        }

        return true;
    }

}
