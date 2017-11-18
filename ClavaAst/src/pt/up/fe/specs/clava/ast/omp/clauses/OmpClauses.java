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

import static pt.up.fe.specs.clava.ast.omp.clauses.OmpClauseKind.*;

import java.util.List;
import java.util.Optional;

import pt.up.fe.specs.clava.ast.omp.OmpPragma;
import pt.up.fe.specs.util.SpecsCollections;

public class OmpClauses {

    public static Optional<OmpProcBindClause> getProcBind(OmpPragma ompPragma) {
        return ompPragma.getClause(OmpClauseKind.PROC_BIND)
                .flatMap(OmpClauses::returnFirst)
                .map(OmpProcBindClause.class::cast);
    }

    public static Optional<OmpIntegerExpressionClause> getNumThreads(OmpPragma ompPragma) {
        return ompPragma.getClause(OmpClauseKind.NUM_THREADS)
                .flatMap(OmpClauses::returnFirst)
                .map(OmpIntegerExpressionClause.class::cast);
    }

    public static Optional<List<OmpListClause>> getListClause(OmpPragma ompPragma, OmpClauseKind kind) {
        return ompPragma.getClause(kind)
                .map(clausesList -> SpecsCollections.cast(clausesList, OmpListClause.class));
    }

    private static <K> Optional<K> returnFirst(List<K> list) {
        if (list.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(list.get(0));
    }

    public static OmpIntegerExpressionClause newNumThreads(String expression) {
        return new OmpIntegerExpressionClause(NUM_THREADS, expression, false, false);
    }

}
