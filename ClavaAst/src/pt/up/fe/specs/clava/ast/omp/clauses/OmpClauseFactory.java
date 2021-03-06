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

/**
 * Methods to build new OpenMP clause objects.
 * 
 * @author JoaoBispo
 *
 */
public class OmpClauseFactory {

    public static OmpIntegerExpressionClause numThreads(String expression) {
        return new OmpIntegerExpressionClause(NUM_THREADS, expression, false, false);
    }

    public static OmpIntegerExpressionClause collapse(String expression) {
        return new OmpIntegerExpressionClause(COLLAPSE, expression, false, true);
    }

    public static OmpIntegerExpressionClause ordered(String expression) {
        return new OmpIntegerExpressionClause(ORDERED, expression, true, true);
    }

}
