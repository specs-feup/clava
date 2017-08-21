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

/**
 *
 * @author joao bispo
 *
 */
public class OmpListClause implements OmpClause {

    private final OmpClauseKind kind;
    private final List<String> variables;

    public OmpListClause(OmpClauseKind kind, List<String> variables) {
        this.kind = kind;
        this.variables = variables;
    }

    public List<String> getVariables() {
        return variables;
    }

    /**
     * Generates code as in OpenMP 4.5:<br>
     * private(list)
     */
    @Override
    public String getCode() {
        return getKind().getString() + "(" +
                variables.stream().collect(Collectors.joining(", "))
                + ")";
    }

    @Override
    public OmpClauseKind getKind() {
        return kind;
    }

}
