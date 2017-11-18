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

import com.google.common.base.Preconditions;

public class OmpIntegerExpressionClause implements OmpClause {

    private final OmpClauseKind kind;
    private final String expression;
    private final boolean isOptional;
    private final boolean isConstantPositive;

    public OmpIntegerExpressionClause(OmpClauseKind kind, String expression, boolean isOptional,
            boolean isConstantPositive) {

        this.kind = kind;
        this.expression = expression;
        this.isOptional = isOptional;
        this.isConstantPositive = isConstantPositive;

        if (!isOptional) {
            Preconditions.checkNotNull(expression, "Expected expression to be present, since it is not optional");
        }

        // TODO: Verify if positive constant integer expression with Symja?
    }

    /**
     * 
     * @return an integer expression. Throws an exception if no expression is defined
     */
    public String getExpression() {
        if (expression == null) {
            throw new RuntimeException("No expression is defined");
        }
        return expression;
    }

    public boolean hasExpression() {
        return expression != null;
    }

    @Override
    public OmpClauseKind getKind() {
        return kind;
    }

    public boolean isConstantPositive() {
        return isConstantPositive;
    }

    public boolean isOptional() {
        return isOptional;
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();
        code.append(kind.getString());

        if (hasExpression()) {
            code.append("(").append(expression).append(")");
        }

        return code.toString();
    }

}
