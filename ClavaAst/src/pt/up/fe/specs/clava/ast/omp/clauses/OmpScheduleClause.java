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

package pt.up.fe.specs.clava.ast.omp.clauses;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

/**
 * Represents a 'schedule' clause, e.g.: #pragma omp for schedule(static, 3)
 * 
 * 
 * @author Joao Bispo
 *
 */

public class OmpScheduleClause implements OmpClause {

    public enum ScheduleKind implements StringProvider {
        STATIC,
        DYNAMIC,
        GUIDED,
        AUTO,
        RUNTIME;

        private static final Lazy<EnumHelper<ScheduleKind>> ENUM_HELPER = EnumHelper.newLazyHelper(ScheduleKind.class);

        public static EnumHelper<ScheduleKind> getHelper() {
            return ENUM_HELPER.get();
        }

        @Override
        public String getString() {
            return name().toLowerCase();
        }
    }

    public enum ScheduleModifier implements StringProvider {
        MONOTONIC,
        NONMONOTONIC,
        SIMD;

        private static final Lazy<EnumHelper<ScheduleModifier>> ENUM_HELPER = EnumHelper
                .newLazyHelper(ScheduleModifier.class);

        public static EnumHelper<ScheduleModifier> getHelper() {
            return ENUM_HELPER.get();
        }

        @Override
        public String getString() {
            return name().toLowerCase();
        }
    }

    private final ScheduleKind schedule;
    private final Integer chunkSize;
    private final List<ScheduleModifier> modifiers;

    public OmpScheduleClause(ScheduleKind schedule, Integer chunkSize, List<ScheduleModifier> modifiers) {
        this.schedule = schedule;
        this.chunkSize = chunkSize;
        this.modifiers = modifiers;

        if (chunkSize != null) {
            Preconditions.checkArgument(chunkSize > 0, "Chunck size must be a positive integer");
        }

    }

    @Override
    public OmpClauseKind getKind() {
        return OmpClauseKind.SCHEDULE;
    }

    /**
     * Generates code as in OpenMP 4.5:<br>
     * schedule([modifier [, modifier]:]kind[, chunk_size])
     */
    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append("schedule(");

        // Modifiers
        if (!modifiers.isEmpty()) {
            String modifiersCode = modifiers.stream()
                    .map(modifier -> modifier.getString())
                    .collect(Collectors.joining(", ", "", ":"));

            code.append(modifiersCode);
        }

        // Kind
        code.append(schedule.getString());

        // Chuck Size
        if (chunkSize != null) {
            code.append(", ").append(chunkSize);
        }

        code.append(")");

        return code.toString();
    }

}
