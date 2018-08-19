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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClauseKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClauses;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.util.SpecsLogs;

public abstract class OmpPragma extends Pragma {

    /// DATAKEYS BEGIN

    public final static DataKey<OmpDirectiveKind> DIRECTIVE_KIND = KeyFactory.enumeration("directiveKind",
            OmpDirectiveKind.class);

    /// DATAKEYS END
    // private final OmpDirectiveKind directiveKind;
    // private Lazy<OmpClauses> clauses;
    private OmpClauses clauses;

    protected OmpPragma(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

        clauses = null;
    }

    // /**
    // * @deprecated
    // * @param directiveKind
    // * @param info
    // */
    // @Deprecated
    // protected OmpPragma(OmpDirectiveKind directiveKind, ClavaNodeInfo info) {
    //
    // super(info, Collections.emptyList());
    //
    // this.directiveKind = directiveKind;
    // this.clauses = Lazy.newInstance(() -> new OmpClauses(this));
    // }

    public OmpClauses clauses() {
        if (clauses == null) {
            clauses = new OmpClauses(this);
        }

        return clauses;
    }

    public OmpDirectiveKind getDirectiveKind() {
        return get(DIRECTIVE_KIND);
        // return directiveKind;
    }

    public List<OmpClause> getClause(OmpClauseKind clauseKind) {
        return Collections.emptyList();
    }

    public List<OmpClauseKind> getClauseKinds() {
        return Collections.emptyList();
    }

    //
    // public List<OmpClause> getClauseOrCreate(OmpClauseKind clauseKind, Supplier<OmpClause> supplier) {
    // throw new RuntimeException("Not implemented yet");
    // }

    public Boolean hasClause(OmpClauseKind clauseKind) {
        return false;
    }

    /**
     * By default this does nothing.
     *
     * @param numThreads
     * @param clause
     */
    public void addClause(OmpClauseKind kind, OmpClause clause) {

    }

    @Override
    public void setFullContent(String fullContent) {
        SpecsLogs.msgWarn("Pragma.setFullContent is not supported for OmpPragma, please use the setters");
    }

    public void setClause(OmpClause ompClause) {
        // Does nothing
        // SpecsLogs.msgInfo("Class " + getClass() + " does not support setClause(OmpClause)");
    }

    public void setClause(List<OmpClause> clauseList) {
        // Does nothing
        // SpecsLogs.msgInfo("Class " + getClass() + " does not support setClause(List<OmpClause>)");
    }

    public void removeClause(OmpClauseKind clauseKind) {
        // Does nothing
        // SpecsLogs.msgInfo("Class " + getClass() + " does not support removeClause()");
    }
}
