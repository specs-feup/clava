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

package pt.up.fe.specs.clava.omp;

import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.omp.clause.OMPClause;

/**
 * Represents the information inside an OpenMP directive.
 * 
 * @author Jo�o Bispo
 *
 */
public class OMPDirective {

    private final String directive;
    private final List<OMPClause> clauses;

    public OMPDirective(String directive, List<OMPClause> clauses) {
        this.directive = directive;
        this.clauses = clauses;
    }

    public String getString() {
        return directive;
    }

    public List<OMPClause> getClauses() {
        return clauses;
    }

    public String getCode(String prologue) {
        StringBuilder code = new StringBuilder();

        code.append("#pragma omp ");
        code.append(prologue);

        if (!clauses.isEmpty()) {
            String clausesCode = clauses.stream()
                    .map(clause -> clause.getCode())
                    .collect(Collectors.joining(" ", " ", ""));

            code.append(clausesCode);
        }
        return code.toString();
    }

}
