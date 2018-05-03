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

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public class OmpReductionClause implements OmpClause {

    public enum ReductionKind implements StringProvider {
        ADD("+"),
        SUBTRACT("-"),
        MULTIPLY("*"),
        BIT_AND("&"),
        BIT_OR("|"),
        XOR("^"),
        LOGICAL_AND("&&"),
        LOGICAL_OR("||"),
        MIN("min"),
        MAX("max");

        private static final Lazy<EnumHelperWithValue<ReductionKind>> ENUM_HELPER = EnumHelperWithValue
                .newLazyHelperWithValue(ReductionKind.class);

        public static EnumHelperWithValue<ReductionKind> getHelper() {
            return ENUM_HELPER.get();
        }

        private final String code;

        private ReductionKind(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        @Override
        public String getString() {
            return getCode();
        }

    }

    private final ReductionKind reductionKind;
    private final List<String> variables;

    public OmpReductionClause(ReductionKind reductionKind, List<String> variables) {
        this.reductionKind = reductionKind;
        this.variables = variables;

        Preconditions.checkNotNull(variables);
        Preconditions.checkArgument(variables.size() > 0);
    }

    @Override
    public OmpClauseKind getKind() {
        return OmpClauseKind.REDUCTION;
    }

    @Override
    public String getCode() {

        StringBuilder builder = new StringBuilder("reduction(");

        builder.append(reductionKind.getCode());
        builder.append(" ");

        builder.append(variables.stream().collect(Collectors.joining(", ", ": ", "")));

        builder.append(")");
        return builder.toString();
    }

    public ReductionKind getReductionKind() {
        return reductionKind;
    }

    public List<String> getVariables() {
        return variables;
    }

}
