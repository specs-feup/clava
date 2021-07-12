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

package pt.up.fe.specs.clava.language;

import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum UnaryExprOrTypeTrait implements StringProvider {

    SizeOf("sizeof"),
    AlignOf("alignof"),
    PreferredAlignOf("__alignof"),
    VecStep("vec_step"),
    OpenMPRequiredSimdAlign("__builtin_omp_required_simd_align");

    private static final Lazy<EnumHelperWithValue<UnaryExprOrTypeTrait>> HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(UnaryExprOrTypeTrait.class);

    public static EnumHelperWithValue<UnaryExprOrTypeTrait> getHelper() {
        return HELPER.get();
    }

    private final String code;

    private UnaryExprOrTypeTrait() {
        this.code = name().toLowerCase();
    }

    private UnaryExprOrTypeTrait(String code) {
        this.code = code;
    }

    @Override
    public String getString() {
        return code;
    }
}