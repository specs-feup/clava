/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clava.ast.type.enums;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * @deprecated use C99Qualifier instead
 * @author JoaoBispo
 *
 */
public enum Qualifier {

    CONST,
    RESTRICT,
    VOLATILE,
    GLOBAL,
    CONSTEXPR;

    private static final Set<Qualifier> C99_QUALIFIERS = EnumSet.of(Qualifier.CONST, Qualifier.RESTRICT,
            Qualifier.VOLATILE);

    public String getCode(boolean isCxx) {
        if (this == RESTRICT) {
            // if (App.getCurrentStandard() != null && App.getCurrentStandard().isCxx()) {
            // System.out.println("CURRENT STANDARD:" + App.getCurrentStandard());
            // System.out.println("IS C++:" + App.getCurrentStandard().isCxx());
            // return "__restrict__";
            // }
            if (isCxx) {
                return "__restrict__";
            }
        }

        if (this == GLOBAL) {
            return "__global";
        }

        return name().toLowerCase(Locale.UK);
    }

    public boolean isC99Qualifier() {
        return C99_QUALIFIERS.contains(this);
    }

    public C99Qualifier toC99Qualifier() {
        switch (this) {
        case CONST:
            return C99Qualifier.CONST;
        case RESTRICT:
            return C99Qualifier.RESTRICT;
        case VOLATILE:
            return C99Qualifier.VOLATILE;
        default:
            throw new RuntimeException("Not a C99 Qualifier:" + this);
        }
    }

    public static Qualifier parse(C99Qualifier c99Qualifier) {
        switch (c99Qualifier) {
        case CONST:
            return CONST;
        case RESTRICT:
        case RESTRICT_C99:
            return RESTRICT;
        case VOLATILE:
            return Qualifier.VOLATILE;
        default:
            throw new RuntimeException("Not a Qualifier:" + c99Qualifier);

        }
    }

}
