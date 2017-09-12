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

import java.util.Collections;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClauseKind;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.util.SpecsLogs;

public abstract class OmpPragma extends Pragma {

    private final OmpDirectiveKind directiveKind;

    protected OmpPragma(OmpDirectiveKind directiveKind, ClavaNodeInfo info) {

        super(info, Collections.emptyList());

        this.directiveKind = directiveKind;
    }

    public OmpDirectiveKind getDirectiveKind() {
        return directiveKind;
    }

    public Optional<OmpClause> getClause(OmpClauseKind clauseKind) {
        return Optional.empty();
    }

    public Boolean hasClause(OmpClauseKind clauseKind) {
        return false;
    }

    /**
     * By default this does nothing.
     *
     * @param numThreads
     * @param clause
     */
    public void setClause(OmpClauseKind numThreads, OmpClause clause) {

    }

    @Override
    public void setFullContent(String fullContent) {
        SpecsLogs.msgWarn("Pragma.setFullContent is not supported for OmpPragma, please use the setters");
    }
}
