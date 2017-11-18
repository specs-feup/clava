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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClauseKind;
import pt.up.fe.specs.util.SpecsLogs;

public class OmpClausePragma extends OmpPragma {

    private final Map<OmpClauseKind, List<OmpClause>> clauses;
    private String customContent;

    public OmpClausePragma(OmpDirectiveKind directiveKind, Map<OmpClauseKind, List<OmpClause>> clauses,
            ClavaNodeInfo info) {
        this(directiveKind, null, clauses, info);
    }

    private OmpClausePragma(OmpDirectiveKind directiveKind, String customContent,
            Map<OmpClauseKind, List<OmpClause>> clauses, ClavaNodeInfo info) {
        super(directiveKind, info);

        this.clauses = clauses;
        this.customContent = customContent;
    }

    @Override
    public String getFullContent() {

        // Give priority to custom content
        if (customContent != null) {
            return customContent;
        }

        StringBuilder fullContent = new StringBuilder();

        fullContent.append("omp ");
        fullContent.append(getDirectiveKind().getString());
        fullContent.append(clauses.values().stream()
                .map(clauseList -> clauseList.stream().map(clause -> clause.getCode())
                        .collect(Collectors.joining(" ")))
                .collect(Collectors.joining(" ", " ", "")));

        return fullContent.toString();
    }

    @Override
    protected OmpClausePragma copyPrivate() {
        // We should check if we should have immutable clauses (e.g., replace the clause object, easier in a map)
        // Or mutable clauses (need to copy the clauses here)
        return new OmpClausePragma(getDirectiveKind(), customContent, new HashMap<>(clauses), getInfo());
    }

    @Override
    public List<OmpClause> getClause(OmpClauseKind clauseKind) {
        if (customContent != null) {
            SpecsLogs.msgInfo("OpenMP pragma " + getDirectiveKind()
                    + " has custom content set, no clause processing will be done");
            return Collections.emptyList();
        }

        List<OmpClause> clausesList = clauses.get(clauseKind);

        return clausesList != null ? clausesList : Collections.emptyList();
    }

    /*
    @Override
    public List<OmpClause> getClauseOrCreate(OmpClauseKind clauseKind, Supplier<OmpClause> supplier) {
        Optional<List<OmpClause>> clausesList = getClause(clauseKind);
        if (clausesList.isPresent()) {
            return clausesList.get();
        }
    
        OmpClause newClause = supplier.get();
    
        Preconditions.checkArgument(clauseKind == newClause.getKind(), "Expected a clause of kind '" + clauseKind
                + "', but supplier returned a clause of kind '" + newClause.getKind() + "'");
        addClause(clauseKind, newClause);
    
        return getClause(clauseKind).get();
    }
    */

    @Override
    public void addClause(OmpClauseKind kind, OmpClause clause) {
        List<OmpClause> clausesList = clauses.get(kind);
        if (clausesList == null) {
            clausesList = new ArrayList<>();
            clauses.put(kind, clausesList);
        }

        clausesList.add(clause);
        // clauses.put(kind, clause);
    }

    @Override
    public void setClause(OmpClause ompClause) {
        OmpDirectiveKind directiveKind = getDirectiveKind();

        if (!directiveKind.isClauseLegal(ompClause.getKind())) {
            ClavaLog.info("Can't set '" + ompClause.getKind().getString() + "' value on a " + directiveKind.getString()
                    + " directive.");
            return;
        }

        setClause(Arrays.asList(ompClause));
    }

    @Override
    public void setClause(List<OmpClause> ompClauseList) {
        // Check all clauses have the same kind
        OmpClauseKind firstKind = null;
        for (OmpClause clause : ompClauseList) {

            // If the first, store if for comparison
            if (firstKind == null) {
                firstKind = clause.getKind();
                continue;
            }

            if (firstKind != clause.getKind()) {
                SpecsLogs.msgInfo(
                        "OmpClausePragma.setClause: expected all clauses to have the same kind, but list has kind "
                                + firstKind + " and kind " + clause.getKind());
                return;
            }
        }

        clauses.put(firstKind, ompClauseList);
    }

    @Override
    public Boolean hasClause(OmpClauseKind clauseKind) {
        if (customContent != null) {
            SpecsLogs.msgInfo("OpenMP pragma " + getDirectiveKind()
                    + " has custom content set, no clause processing will be done");
            return false;
        }

        return clauses.containsKey(clauseKind);
    }

    @Override
    public void setFullContent(String fullContent) {
        this.customContent = fullContent;
    }

    @Override
    public List<OmpClauseKind> getClauseKinds() {
        return new ArrayList<>(clauses.keySet());
    }
}
