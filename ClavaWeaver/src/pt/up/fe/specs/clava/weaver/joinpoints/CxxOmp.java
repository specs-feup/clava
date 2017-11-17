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

package pt.up.fe.specs.clava.weaver.joinpoints;

import static pt.up.fe.specs.clava.ast.omp.clauses.OmpClauseKind.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.omp.OmpDirectiveKind;
import pt.up.fe.specs.clava.ast.omp.OmpPragma;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClauseKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClauses;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpListClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpNumThreadsClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpProcBindClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpProcBindClause.ProcBindKind;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AOmp;

public class CxxOmp extends AOmp {

    private final OmpPragma ompPragma;
    private final ACxxWeaverJoinPoint parent;

    public CxxOmp(OmpPragma ompPragma, ACxxWeaverJoinPoint parent) {
        super(new CxxPragma(ompPragma, parent));

        this.ompPragma = ompPragma;
        this.parent = parent;
    }

    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        return parent;
    }

    @Override
    public ClavaNode getNode() {
        return ompPragma;
    }

    @Override
    public String getKindImpl() {
        return ompPragma.getDirectiveKind().getString();
    }

    @Override
    public String getNumThreadsImpl() {
        return OmpClauses.getNumThreads(ompPragma)
                .map(OmpNumThreadsClause::getExpression)
                .orElse(null);

        /*
        Optional<OmpClause> clause = ompPragma.getClause(NUM_THREADS);
        
        return clause
                .map(OmpNumThreadsClause.class::cast)
                .map(OmpNumThreadsClause::getExpression)
                .orElse("");
                */
    }

    @Override
    public String getProcBindImpl() {

        return OmpClauses.getProcBind(ompPragma)
                .map(OmpProcBindClause::getprocBindKindString)
                .orElse(null);
        /*
        Optional<OmpClause> clause = ompPragma.getClause(PROC_BIND);
        
        return clause
                .map(OmpProcBindClause.class::cast)
                .map(OmpProcBindClause::getprocBindKindString)
                .orElse("");
                */
    }

    // @Override
    // public String getNumThreadsImplOld() {
    //
    // OmpDirectiveKind directiveKind = ompPragma.getDirectiveKind();
    //
    // if (directiveKind.isClauseLegal(OmpClauseKind.NUM_THREADS)) {
    //
    // if (ompPragma instanceof OmpClausePragma) { // Clause directives
    //
    // OmpClausePragma clausePragma = (OmpClausePragma) ompPragma;
    // Optional<OmpClause> clause = clausePragma.getClause(OmpClauseKind.NUM_THREADS);
    //
    // return clause
    // .map(OmpNumThreadsClause.class::cast)
    // .map(ompClause -> ompClause.getExpression())
    // .orElse("");
    // } else { // Special directives which are neither simple nor clause directives, e.g., #pragma omp atomic...
    // throw new RuntimeException("Getting num_threads clause from a " + directiveKind + "("
    // + ompPragma.getClass() + ") directive is not implmented yet: "
    // + ompPragma.getLocation());
    // }
    // }
    //
    // // Simple directives or any other directives which do not support the clause
    // ClavaLog.info("Cannot get num_threads clause from a " + directiveKind + " directive: "
    // + ompPragma.getLocation());
    // return "";
    // }

    // @Override
    // public String getProcBindImplOld() {
    //
    // OmpDirectiveKind directiveKind = ompPragma.getDirectiveKind();
    //
    // if (directiveKind.isClauseLegal(OmpClauseKind.PROC_BIND)) {
    //
    // if (ompPragma instanceof OmpClausePragma) { // Clause directives
    //
    // OmpClausePragma clausePragma = (OmpClausePragma) ompPragma;
    // Optional<OmpClause> clause = clausePragma.getClause(OmpClauseKind.PROC_BIND);
    //
    // return clause
    // .map(OmpProcBindClause.class::cast)
    // .map(ompClause -> ompClause.getKind().getString())
    // .orElse("");
    // } else { // Special directives which are neither simple nor clause directives, e.g., #pragma omp atomic...
    // throw new RuntimeException("Getting proc_bind clause from a " + directiveKind + "("
    // + ompPragma.getClass() + ") directive is not implmented yet: "
    // + ompPragma.getLocation());
    // }
    // }
    //
    // // Simple directives or any other directives which do not support the clause
    // ClavaLog.info("Cannot get proc_bind clause from a " + directiveKind + " directive: "
    // + ompPragma.getLocation());
    // return "";
    // }

    @Override
    public Boolean hasClauseImpl(String clauseName) {
        OmpClauseKind clauseKind = OmpClauseKind.getHelper().valueOf(clauseName);
        return ompPragma.hasClause(clauseKind);
    }

    @Override
    public Boolean isClauseLegalImpl(String clauseName) {
        OmpClauseKind clauseKind = OmpClauseKind.getHelper().valueOf(clauseName);
        return ompPragma.getDirectiveKind().isClauseLegal(clauseKind);
    }

    @Override
    public void setNumThreadsImpl(String newExpr) {
        OmpNumThreadsClause clause = new OmpNumThreadsClause(newExpr);
        setClause(clause);
        /*
        OmpDirectiveKind directiveKind = ompPragma.getDirectiveKind();
        
        if (!directiveKind.isClauseLegal(NUM_THREADS)) {
            ClavaLog.info("Can't set '" + NUM_THREADS.getString() + "' value on a " + directiveKind.getString()
                    + " directive.");
            return;
        }
        
        OmpNumThreadsClause clause = new OmpNumThreadsClause(newExpr);
        
        ompPragma.addClause(NUM_THREADS, clause);
        */
    }

    @Override
    public void setProcBindImpl(String newBind) {
        ProcBindKind kind = ProcBindKind.getHelper().valueOfTry(newBind).orElse(null);
        if (kind == null) {
            ClavaLog.info("Can't set '" + newBind + "' as a proc bind value, valid values: "
                    + ProcBindKind.getHelper().getAvailableOptions());
            return;
        }

        setClause(new OmpProcBindClause(kind));

        /*        
        OmpDirectiveKind directiveKind = ompPragma.getDirectiveKind();
        
        if (!directiveKind.isClauseLegal(PROC_BIND)) {
            ClavaLog.info("Can't set '" + PROC_BIND.getString() + "' value on a " + directiveKind.getString()
                    + " directive.");
            return;
        }
        
        ProcBindKind kind = ProcBindKind.getHelper().valueOf(newBind);
        
        OmpProcBindClause clause = new OmpProcBindClause(kind);
        
        ompPragma.addClause(PROC_BIND, clause);
        */
    }

    private void setClause(OmpClause clause) {
        OmpDirectiveKind directiveKind = ompPragma.getDirectiveKind();

        if (!directiveKind.isClauseLegal(clause.getKind())) {
            ClavaLog.info("Can't set '" + clause.getKind().getString() + "' value on a " + directiveKind.getString()
                    + " directive.");
            return;
        }

        ompPragma.setClause(clause);
    }

    @Override
    public String[] getPrivateArrayImpl() {
        Optional<List<OmpListClause>> clauses = OmpClauses.getListClause(ompPragma, PRIVATE);

        if (!clauses.isPresent()) {
            return new String[0];
        }

        return clauses.get().stream()
                .flatMap(clauseList -> clauseList.getVariables().stream())
                .collect(Collectors.toList())
                .toArray(new String[0]);

        // return variables.toArray(new )
        // .toArray(variableList -> new String[variableList.size()]);

        // return OmpClauses.getProcBind(ompPragma)
        // .map(OmpProcBindClause::getprocBindKindString)
        // .orElse("");
    }

    @Override
    public void setPrivateImpl(String[] newVariables) {
        ompPragma.setClause(new OmpListClause(PRIVATE, Arrays.asList(newVariables)));
    }
}
