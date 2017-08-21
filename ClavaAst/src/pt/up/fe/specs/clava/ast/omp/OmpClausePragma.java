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

package pt.up.fe.specs.clava.ast.omp;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClauseKind;

public class OmpClausePragma extends OmpPragma {

    private final Map<OmpClauseKind, OmpClause> clauses;

    public OmpClausePragma(OmpDirectiveKind directiveKind, Map<OmpClauseKind, OmpClause> clauses,
            ClavaNodeInfo info) {
        super(directiveKind, info);

        this.clauses = clauses;
    }

    @Override
    public String getFullContent() {
        StringBuilder fullContent = new StringBuilder();

        fullContent.append("omp ");
        fullContent.append(getDirectiveKind().getString());
        fullContent.append(clauses.values().stream()
                .map(clause -> clause.getCode()).collect(Collectors.joining(" ", " ", "")));

        return fullContent.toString();
    }

    @Override
    protected OmpClausePragma copyPrivate() {
        // We should check if we should have immutable clauses (e.g., replace the clause object, easier in a map)
        // Or mutable clauses (need to copy the clauses here)
        return new OmpClausePragma(getDirectiveKind(), new HashMap<>(clauses), getInfo());
    }

    @Override
    public Optional<OmpClause> getClause(OmpClauseKind clauseKind) {
        return Optional.ofNullable(clauses.get(clauseKind));
    }

    @Override
    public void setClause(OmpClauseKind kind, OmpClause clause) {
        clauses.put(kind, clause);
    }

    @Override
    public Boolean hasClause(OmpClauseKind clauseKind) {
        return clauses.containsKey(clauseKind);
    }

}
