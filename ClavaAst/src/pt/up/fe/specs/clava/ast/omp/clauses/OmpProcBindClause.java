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

import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public class OmpProcBindClause implements OmpClause {

    public enum ProcBindKind implements StringProvider {
        MASTER,
        CLOSE,
        SPREAD;

        private static final Lazy<EnumHelperWithValue<ProcBindKind>> ENUM_HELPER = EnumHelperWithValue
                .newLazyHelperWithValue(ProcBindKind.class);

        public static EnumHelperWithValue<ProcBindKind> getHelper() {
            return ENUM_HELPER.get();
        }

        @Override
        public String getString() {
            return name().toLowerCase();
        }

    }

    private final ProcBindKind kind;

    public OmpProcBindClause(ProcBindKind kind) {
        this.kind = kind;
    }

    @Override
    public OmpClauseKind getKind() {
        return OmpClauseKind.PROC_BIND;
    }

    @Override
    public String getCode() {
        return "proc_bind(" + kind.getString() + ")";
    }

    public String getprocBindKindString() {
        return getProcBindKind().getString();
    }

    public ProcBindKind getProcBindKind() {
        return kind;
    }

}
