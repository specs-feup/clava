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

import java.util.Arrays;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.omp.OmpDirectiveKind;
import pt.up.fe.specs.clava.ast.omp.OmpPragma;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClauseKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpDefaultClause.DefaultKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpProcBindClause.ProcBindKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpReductionClause.ReductionKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpScheduleClause.ScheduleKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpScheduleClause.ScheduleModifier;
import pt.up.fe.specs.clava.parsing.omp.OmpParser;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AOmp;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class CxxOmp extends AOmp {

    private OmpPragma ompPragma;

    public CxxOmp(OmpPragma ompPragma) {
        super(new CxxPragma(ompPragma));

        this.ompPragma = ompPragma;
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
        return ompPragma.clauses().getNumThreads().orElse(null);
    }

    @Override
    public String getProcBindImpl() {
        return ompPragma.clauses().getProcBind()
                .map(ProcBindKind::getKey)
                .orElse(null);
    }

    @Override
    public Boolean hasClauseImpl(String clauseName) {
        OmpClauseKind clauseKind = parseClauseName(clauseName);
        // if (clauseKind == null) {
        // return false;
        // }

        return ompPragma.hasClause(clauseKind);
    }

    private OmpClauseKind parseClauseName(String clauseName) {
        return OmpClauseKind.getHelper().fromValue(clauseName);
        // OmpClauseKind clauseKind = OmpClauseKind.getHelper().valueOfTry(clauseName).orElse(null);
        // if (clauseKind == null) {
        //
        // }
        // return clauseKind;
    }

    @Override
    public Boolean isClauseLegalImpl(String clauseName) {
        OmpClauseKind clauseKind = parseClauseName(clauseName);
        // if (clauseKind == null) {
        // return false;
        // }

        return ompPragma.getDirectiveKind().isClauseLegal(clauseKind);
    }

    @Override
    public void setNumThreadsImpl(String newExpr) {
        ompPragma.clauses().setNumThreads(newExpr);
    }

    @Override
    public void setProcBindImpl(String newBind) {
        ProcBindKind kind = ProcBindKind.getHelper().fromValueTry(newBind)
                .orElseThrow(() -> new RuntimeException("Can't set '" + newBind
                        + "' as a proc bind value, valid values: " + ProcBindKind.getHelper().getAvailableValues()));
        ompPragma.clauses().setProcBind(kind);
        // ProcBindKind kind = ProcBindKind.getHelper().valueOfTry(newBind).orElse(null);
        // if (kind == null) {
        // ClavaLog.info("Can't set '" + newBind + "' as a proc bind value, valid values: "
        // + ProcBindKind.getHelper().getAvailableOptions());
        // return;
        // }

        // setClause(new OmpProcBindClause(kind));

    }

    // private void setClause(OmpClause clause) {
    // ompPragma.setClause(clause);
    // }

    @Override
    public String[] getPrivateArrayImpl() {
        return ompPragma.clauses().getPrivate().toArray(new String[0]);
    }

    @Override
    public void setPrivateImpl(String[] newVariables) {
        ompPragma.clauses().setPrivate(Arrays.asList(newVariables));
    }

    @Override
    public String[] getClauseKindsArrayImpl() {
        return SpecsCollections.toStringArray(ompPragma.getClauseKinds());

        // return ompPragma.getClauseKinds().stream()
        // .map(OmpClauseKind::getKey)
        // .collect(Collectors.toList())
        // .toArray(new String[0]);
    }

    @Override
    public String[] reductionArrayImpl(String kind) {
        return ompPragma.clauses().getReduction(kind).toArray(new String[0]);
    }

    @Override
    public void setReductionImpl(String reductionKindString, String[] newVariables) {
        ReductionKind reductionKind = ReductionKind.getHelper().fromValue(reductionKindString.toLowerCase());

        ompPragma.clauses().setReduction(reductionKind, Arrays.asList(newVariables));
    }

    @Override
    public String[] getReductionKindsArrayImpl() {
        return SpecsCollections.toStringArray(ompPragma.clauses().getReductionKinds());
        // String[] a = SpecsCollections.toStringArray(ompPragma.clauses().getReductionKinds());
        // return ompPragma.clauses().getReductionKinds().stream()
        // .map(ReductionKind::getKey)
        // .collect(Collectors.toList())
        // .toArray(new String[0]);
    }

    @Override
    public String getDefaultImpl() {
        return ompPragma.clauses().getDefault()
                .map(DefaultKind::getKey)
                .orElse(null);
    }

    @Override
    public void setDefaultImpl(String newDefault) {
        DefaultKind kind = DefaultKind.getHelper().fromValueTry(newDefault)
                .orElseThrow(() -> new RuntimeException("Can't set '" + newDefault
                        + "' as a 'default' value, valid values: " + DefaultKind.getHelper().getAvailableValues()));
        ompPragma.clauses().setDefault(kind);
    }

    @Override
    public String[] getFirstprivateArrayImpl() {
        return ompPragma.clauses().getFirstprivate().toArray(new String[0]);
    }

    @Override
    public void setFirstprivateImpl(String[] newVariables) {
        ompPragma.clauses().setFirstprivate(Arrays.asList(newVariables));
    }

    @Override
    public String[] getLastprivateArrayImpl() {
        return ompPragma.clauses().getLastprivate().toArray(new String[0]);
    }

    @Override
    public void setLastprivateImpl(String[] newVariables) {
        ompPragma.clauses().setLastprivate(Arrays.asList(newVariables));
    }

    @Override
    public String[] getSharedArrayImpl() {
        return ompPragma.clauses().getShared().toArray(new String[0]);
    }

    @Override
    public void setSharedImpl(String[] newVariables) {
        ompPragma.clauses().setShared(Arrays.asList(newVariables));
    }

    @Override
    public String[] getCopyinArrayImpl() {
        return ompPragma.clauses().getCopyin().toArray(new String[0]);
    }

    @Override
    public void setCopyinImpl(String[] newVariables) {
        ompPragma.clauses().setCopyin(Arrays.asList(newVariables));
    }

    @Override
    public String getScheduleKindImpl() {
        return ompPragma.clauses().getScheduleKind().map(ScheduleKind::getKey).orElse(null);
    }

    @Override
    public void setScheduleKindImpl(String scheduleKindString) {
        ScheduleKind kind = ScheduleKind.getHelper().fromValueTry(scheduleKindString)
                .orElseThrow(() -> new RuntimeException("Can't set '" + scheduleKindString
                        + "' as a schedule kind, valid values: " + ScheduleKind.getHelper().getAvailableValues()));

        ompPragma.clauses().setScheduleKind(kind);
    }

    @Override
    public String getScheduleChunkSizeImpl() {
        return ompPragma.clauses().getScheduleChunkSize().orElse(null);
    }

    @Override
    public void setScheduleChunkSizeImpl(String chunkSize) {
        ompPragma.clauses().setScheduleChunkSize(chunkSize);
    }

    @Override
    public void setScheduleChunkSizeImpl(Integer chunkSize) {
        setScheduleChunkSize(chunkSize.toString());
    }

    @Override
    public String[] getScheduleModifiersArrayImpl() {
        return SpecsCollections.toStringArray(ompPragma.clauses().getScheduleModifiers());
    }

    @Override
    public void setScheduleModifiersImpl(String[] modifiers) {
        List<ScheduleModifier> parsedModifiers = ScheduleModifier.getHelper().fromValue(Arrays.asList(modifiers));
        ompPragma.clauses().setScheduleModifiers(parsedModifiers);
    }

    @Override
    public String getCollapseImpl() {
        return ompPragma.clauses().getCollapse().orElse(null);
    }

    @Override
    public void setCollapseImpl(String newExpr) {
        ompPragma.clauses().setCollapse(newExpr);
    }

    @Override
    public void setCollapseImpl(Integer newExpr) {
        setCollapseImpl(newExpr.toString());
    }

    @Override
    public String getOrderedImpl() {
        return ompPragma.clauses().getOrdered().orElse(null);

    }

    @Override
    public void setOrderedImpl(String newExpr) {
        ompPragma.clauses().setOrdered(newExpr);
    }

    @Override
    public void removeClauseImpl(String clauseKindString) {
        OmpClauseKind clauseKind = OmpClauseKind.getHelper().fromValueTry(clauseKindString)
                .orElseThrow(() -> new RuntimeException("Can't remove clause '" + clauseKindString
                        + "', name is not valid. Valid clause names: "
                        + OmpClauseKind.getHelper().getAvailableValues()));

        ompPragma.removeClause(clauseKind);
    }

    @Override
    public void setKindImpl(String directiveKindString) {
        OmpDirectiveKind directiveKind = OmpDirectiveKind.getHelper().fromValueTry(directiveKindString)
                .orElseThrow(() -> new RuntimeException("Can't set directive kind '" + directiveKindString
                        + "', name is not valid. Valid directive names: "
                        + OmpDirectiveKind.getHelper().getAvailableValues()));

        // Create new pragma based on the previous pragma
        OmpPragma newOmpPragma = OmpParser.newOmpPragma(directiveKind, ompPragma);

        // Replace previous pragma
        NodeInsertUtils.replace(ompPragma, newOmpPragma);

        // Update join point pragma
        this.ompPragma = newOmpPragma;

        // Update parent join point
        this.aPragma = new CxxPragma(ompPragma);
    }

    @Override
    public void defNumThreadsImpl(String value) {
        setNumThreads(value);
    }

    @Override
    public void defProcBindImpl(String value) {
        setProcBind(value);
    }
}
