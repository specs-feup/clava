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

import java.util.Optional;

import com.google.common.base.Preconditions;

public class OmpIfClause implements OmpClause {

    private final String directiveName;
    private final String expression;

    public OmpIfClause(String expression) {
        this(null, expression);
    }

    public OmpIfClause(String directiveName, String expression) {
        this.directiveName = directiveName;
        this.expression = expression;

        Preconditions.checkNotNull(expression);
    }

    public String getExpression() {
        return expression;
    }

    public Optional<String> getDirectiveName() {
        return Optional.ofNullable(directiveName);
    }

    @Override
    public OmpClauseKind getKind() {
        return OmpClauseKind.IF;
    }

    @Override
    public String getCode() {
        String name = directiveName != null ? directiveName + ":" : "";
        return "if(" + name + expression + ")";
    }

}
