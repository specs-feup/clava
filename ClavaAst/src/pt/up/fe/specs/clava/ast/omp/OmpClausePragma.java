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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClauseKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpListClause;
import pt.up.fe.specs.util.SpecsLogs;

public class OmpClausePragma extends OmpPragma {

    /// DATAKEYS BEGIN

    public final static DataKey<Map<OmpClauseKind, List<OmpClause>>> CLAUSES = KeyFactory.generic("clauses",
            new HashMap<OmpClauseKind, List<OmpClause>>());

    public final static DataKey<String> CUSTOM_CONTENT = KeyFactory.string("customContent");

    /// DATAKEYS END

    public OmpClausePragma(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Optional<String> getCustomContent() {
        return getTry(CUSTOM_CONTENT);
    }

    @Override
    public String getFullContent() {

        // Give priority to custom content
        if (hasValue(CUSTOM_CONTENT)) {
            return get(CUSTOM_CONTENT);
        }

        StringBuilder fullContent = new StringBuilder();

        fullContent.append("omp ");
        fullContent.append(getDirectiveKind().getString());
        fullContent.append(get(CLAUSES).values().stream()
                .map(clauseList -> clauseList.stream().map(clause -> clause.getCode())
                        .collect(Collectors.joining(" ")))
                .collect(Collectors.joining(" ", " ", "")));

        return fullContent.toString();
    }

    @Override
    public List<OmpClause> getClause(OmpClauseKind clauseKind) {
        // if (customContent != null) {
        if (getCustomContent().isPresent()) {
            SpecsLogs.msgInfo("OpenMP pragma " + getDirectiveKind()
                    + " has custom content set, no clause processing will be done");
            return Collections.emptyList();
        }

        List<OmpClause> clausesList = get(CLAUSES).get(clauseKind);

        return clausesList != null ? clausesList : Collections.emptyList();
    }

    @Override
    public void addClause(OmpClauseKind kind, OmpClause clause) {
        List<OmpClause> clausesList = get(CLAUSES).get(kind);
        if (clausesList == null) {
            clausesList = new ArrayList<>();
            get(CLAUSES).put(kind, clausesList);
        }

        clausesList.add(clause);
    }

    @Override
    public void setClause(OmpClause ompClause) {
        OmpDirectiveKind directiveKind = getDirectiveKind();

        if (!directiveKind.isClauseLegal(ompClause.getKind())) {
            ClavaLog.info("Can't set '" + ompClause.getKind().getString() + "' value on a " + directiveKind.getString()
                    + " directive.");
            return;
        }

        // Special case: OmpClause is a OmpListClause, and the list is empty. In this case, remove the clause
        if (ompClause instanceof OmpListClause && ((OmpListClause) ompClause).getVariables().isEmpty()) {
            removeClause(ompClause.getKind());
            return;
        }

        setClause(Arrays.asList(ompClause));
    }

    @Override
    public void removeClause(OmpClauseKind kind) {
        get(CLAUSES).remove(kind);

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

        get(CLAUSES).put(firstKind, ompClauseList);
    }

    @Override
    public Boolean hasClause(OmpClauseKind clauseKind) {

        if (getCustomContent().isPresent()) {
            SpecsLogs.msgInfo("OpenMP pragma " + getDirectiveKind()
                    + " has custom content set, no clause processing will be done");
            return false;
        }

        return get(CLAUSES).containsKey(clauseKind);
    }

    @Override
    public void setFullContent(String fullContent) {
        set(CUSTOM_CONTENT, fullContent);
    }

    @Override
    public List<OmpClauseKind> getClauseKinds() {
        return new ArrayList<>(get(CLAUSES).keySet());
    }
}
