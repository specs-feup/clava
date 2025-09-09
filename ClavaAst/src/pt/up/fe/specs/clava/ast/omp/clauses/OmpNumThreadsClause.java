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

import java.util.Objects;

public class OmpNumThreadsClause implements OmpClause {

    private final String expression;

    public OmpNumThreadsClause(String expression) {
        this.expression = expression;

        Objects.requireNonNull(expression);
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public OmpClauseKind getKind() {
        return OmpClauseKind.NUM_THREADS;
    }

    @Override
    public String getCode() {
        return "num_threads(" + expression + ")";
    }

}
