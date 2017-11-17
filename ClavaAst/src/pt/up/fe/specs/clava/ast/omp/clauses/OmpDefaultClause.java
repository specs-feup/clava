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

package pt.up.fe.specs.clava.ast.omp.clauses;

import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public class OmpDefaultClause implements OmpClause {

    public enum DefaultKind implements StringProvider {
        SHARED,
        NONE;

        private static final Lazy<EnumHelper<DefaultKind>> ENUM_HELPER = EnumHelper
                .newLazyHelper(DefaultKind.class);

        public static EnumHelper<DefaultKind> getHelper() {
            return ENUM_HELPER.get();
        }

        @Override
        public String getString() {
            return name().toLowerCase();
        }

    }

    private final DefaultKind kind;

    public OmpDefaultClause(DefaultKind kind) {
        this.kind = kind;
    }

    @Override
    public OmpClauseKind getKind() {
        return OmpClauseKind.DEFAULT;
    }

    @Override
    public String getCode() {
        return "default(" + kind.getString() + ")";
    }

    public String getDefaultKindString() {
        return getDefaultKind().getString();
    }

    public DefaultKind getDefaultKind() {
        return kind;
    }

}
