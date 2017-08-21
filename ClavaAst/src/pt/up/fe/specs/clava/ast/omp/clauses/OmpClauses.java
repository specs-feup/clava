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

import pt.up.fe.specs.clava.ast.omp.OmpPragma;

public class OmpClauses {

    public static Optional<OmpProcBindClause> getProcBind(OmpPragma ompPragma) {
        return ompPragma.getClause(OmpClauseKind.PROC_BIND)
                .map(OmpProcBindClause.class::cast);
    }

    public static Optional<OmpNumThreadsClause> getNumThreads(OmpPragma ompPragma) {
        return ompPragma.getClause(OmpClauseKind.NUM_THREADS)
                .map(OmpNumThreadsClause.class::cast);
    }

}
