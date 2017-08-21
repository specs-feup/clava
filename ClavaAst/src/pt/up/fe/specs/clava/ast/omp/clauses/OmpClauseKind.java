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

public enum OmpClauseKind implements StringProvider {

    ALIGNED,
    COLLAPSE,
    COPYIN,
    COPYPRIVATE,
    DEFAULT,
    DEPEND,
    DEVICE,
    DIST_SCHEDULE,
    FINAL,
    FIRSTPRIVATE,
    FROM,
    IF,
    INBRANCH(false),
    MERGEABLE(false),
    NOTINBRANCH(false),
    NUM_TEAMS,
    NUM_THREADS,
    LASTPRIVATE,
    LINEAR,
    MAP,
    NOWAIT(false),
    ORDERED(false),
    PRIVATE,
    PROC_BIND,
    REDUCTION,
    SAFELEN,
    SCHEDULE,
    SHARED,
    SIMDLEN,
    THREAD_LIMIT,
    TO,
    UNIFORM,
    UNTIED(false),
    PRIORITY,
    GRAINSIZE,
    NOGROUP(false),
    NUM_TASKS,
    USE_DEVICE_PTR,
    IS_DEVICE_PTR,
    DEFAULTMAP,
    LINK,
    THREADS(false),
    SIMD(false);

    private final boolean hasParameters;

    private OmpClauseKind() {
        this(true);
    }

    private OmpClauseKind(boolean hasParameters) {
        this.hasParameters = hasParameters;
    }

    private static final Lazy<EnumHelper<OmpClauseKind>> HELPER = EnumHelper.newLazyHelper(OmpClauseKind.class);

    public static EnumHelper<OmpClauseKind> getHelper() {
        return HELPER.get();
    }

    public boolean hasParameters() {
        return hasParameters;
    }

    @Override
    public String getString() {
        return name().toLowerCase();
    }
}
