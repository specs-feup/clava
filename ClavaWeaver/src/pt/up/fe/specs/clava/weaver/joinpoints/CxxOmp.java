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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.Arrays;
import java.util.List;

import pt.up.fe.specs.clava.ast.omp.OmpDirectiveKind;
import pt.up.fe.specs.clava.ast.omp.OmpPragma;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClauseKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpDefaultClause.DefaultKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpProcBindClause.ProcBindKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpReductionClause.ReductionKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpScheduleClause.ScheduleKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpScheduleClause.ScheduleModifier;
import pt.up.fe.specs.clava.parsing.omp.OmpParser;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AOmp;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class CxxOmp<Self extends CxxOmp<Self>> extends AOmp<Self> {

    public CxxOmp(OmpPragma ompPragma, CxxWeaver weaver) {
        super(ompPragma, weaver);
    }

    @Override
    public OmpPragma getNodeImpl() {
        return (OmpPragma) super.getNodeImpl();
    }

    @Override
    public String getKindImpl() {
        return this.getNodeImpl().getDirectiveKind().getString();
    }

    @Override
    public String getNumThreadsImpl() {
        return this.getNodeImpl().clauses().getNumThreads().orElse(null);
    }

    @Override
    public String getProcBindImpl() {
        return this.getNodeImpl().clauses().getProcBind()
                .map(ProcBindKind::getKey)
                .orElse(null);
    }

    @Override
    public boolean getHasClauseImpl(String clauseName) {
        OmpClauseKind clauseKind = parseClauseName(clauseName);
        return this.getNodeImpl().hasClause(clauseKind);
    }

    private OmpClauseKind parseClauseName(String clauseName) {
        return OmpClauseKind.getHelper().fromValue(clauseName);
    }

    @Override
    public boolean getIsClauseLegalImpl(String clauseName) {
        OmpClauseKind clauseKind = parseClauseName(clauseName);
        return this.getNodeImpl().getDirectiveKind().isClauseLegal(clauseKind);
    }

    @Override
    public void setNumThreadsImpl(String newExpr) {
        this.getNodeImpl().clauses().setNumThreads(newExpr);
    }

    @Override
    public void setProcBindImpl(String newBind) {
        ProcBindKind kind = ProcBindKind.getHelper().fromValueTry(newBind)
                .orElseThrow(() -> new RuntimeException("Can't set '" + newBind
                        + "' as a proc bind value, valid values: " + ProcBindKind.getHelper().getAvailableValues()));
        this.getNodeImpl().clauses().setProcBind(kind);
    }

    @Override
    public String[] getPrivateImpl() {
        return this.getNodeImpl().clauses().getPrivate().toArray(new String[0]);
    }

    @Override
    public void setPrivateImpl(String[] newVariables) {
        this.getNodeImpl().clauses().setPrivate(Arrays.asList(newVariables));
    }

    @Override
    public String[] getClauseKindsImpl() {
        return SpecsCollections.toStringArray(this.getNodeImpl().getClauseKinds());
    }

    @Override
    public String[] getGetReductionImpl(String kind) {
        return this.getNodeImpl().clauses().getReduction(kind).toArray(new String[0]);
    }

    @Override
    public void setReductionImpl(String reductionKindString, String[] newVariables) {
        ReductionKind reductionKind = ReductionKind.getHelper().fromValue(reductionKindString.toLowerCase());

        this.getNodeImpl().clauses().setReduction(reductionKind, Arrays.asList(newVariables));
    }

    @Override
    public String[] getReductionKindsImpl() {
        return SpecsCollections.toStringArray(this.getNodeImpl().clauses().getReductionKinds());
    }

    @Override
    public String getDefaultImpl() {
        return this.getNodeImpl().clauses().getDefault()
                .map(DefaultKind::getKey)
                .orElse(null);
    }

    @Override
    public void setDefaultImpl(String newDefault) {
        DefaultKind kind = DefaultKind.getHelper().fromValueTry(newDefault)
                .orElseThrow(() -> new RuntimeException("Can't set '" + newDefault
                        + "' as a 'default' value, valid values: " + DefaultKind.getHelper().getAvailableValues()));
        this.getNodeImpl().clauses().setDefault(kind);
    }

    @Override
    public String[] getFirstprivateImpl() {
        return this.getNodeImpl().clauses().getFirstprivate().toArray(new String[0]);
    }

    @Override
    public void setFirstprivateImpl(String[] newVariables) {
        this.getNodeImpl().clauses().setFirstprivate(Arrays.asList(newVariables));
    }

    @Override
    public String[] getLastprivateImpl() {
        return this.getNodeImpl().clauses().getLastprivate().toArray(new String[0]);
    }

    @Override
    public void setLastprivateImpl(String[] newVariables) {
        this.getNodeImpl().clauses().setLastprivate(Arrays.asList(newVariables));
    }

    @Override
    public String[] getSharedImpl() {
        return this.getNodeImpl().clauses().getShared().toArray(new String[0]);
    }

    @Override
    public void setSharedImpl(String[] newVariables) {
        this.getNodeImpl().clauses().setShared(Arrays.asList(newVariables));
    }

    @Override
    public String[] getCopyinImpl() {
        return this.getNodeImpl().clauses().getCopyin().toArray(new String[0]);
    }

    @Override
    public void setCopyinImpl(String[] newVariables) {
        this.getNodeImpl().clauses().setCopyin(Arrays.asList(newVariables));
    }

    @Override
    public String getScheduleKindImpl() {
        return this.getNodeImpl().clauses().getScheduleKind().map(ScheduleKind::getKey).orElse(null);
    }

    @Override
    public void setScheduleKindImpl(String scheduleKindString) {
        ScheduleKind kind = ScheduleKind.getHelper().fromValueTry(scheduleKindString)
                .orElseThrow(() -> new RuntimeException("Can't set '" + scheduleKindString
                        + "' as a schedule kind, valid values: " + ScheduleKind.getHelper().getAvailableValues()));

        this.getNodeImpl().clauses().setScheduleKind(kind);
    }

    @Override
    public String getScheduleChunkSizeImpl() {
        return this.getNodeImpl().clauses().getScheduleChunkSize().orElse(null);
    }

    @Override
    public void setScheduleChunkSizeImpl(String chunkSize) {
        this.getNodeImpl().clauses().setScheduleChunkSize(chunkSize);
    }

    @Override
    public void setScheduleChunkSizeImpl(int chunkSize) {
        this.setScheduleChunkSizeImpl(Integer.toString(chunkSize));
    }

    @Override
    public String[] getScheduleModifiersImpl() {
        return SpecsCollections.toStringArray(this.getNodeImpl().clauses().getScheduleModifiers());
    }

    @Override
    public void setScheduleModifiersImpl(String[] modifiers) {
        List<ScheduleModifier> parsedModifiers = ScheduleModifier.getHelper().fromValue(Arrays.asList(modifiers));
        this.getNodeImpl().clauses().setScheduleModifiers(parsedModifiers);
    }

    @Override
    public String getCollapseImpl() {
        return this.getNodeImpl().clauses().getCollapse().orElse(null);
    }

    @Override
    public void setCollapseImpl(String newExpr) {
        this.getNodeImpl().clauses().setCollapse(newExpr);
    }

    @Override
    public void setCollapseImpl(int newExpr) {
        setCollapseImpl(Integer.toString(newExpr));
    }

    @Override
    public String getOrderedImpl() {
        return this.getNodeImpl().clauses().getOrdered().orElse(null);
    }

    @Override
    public void setOrderedImpl(String newExpr) {
        this.getNodeImpl().clauses().setOrdered(newExpr);
    }

    @Override
    public void removeClauseImpl(String clauseKindString) {
        OmpClauseKind clauseKind = OmpClauseKind.getHelper().fromValueTry(clauseKindString)
                .orElseThrow(() -> new RuntimeException("Can't remove clause '" + clauseKindString
                        + "', name is not valid. Valid clause names: "
                        + OmpClauseKind.getHelper().getAvailableValues()));

        this.getNodeImpl().removeClause(clauseKind);
    }

    @Override
    public void setKindImpl(String directiveKindString) {
        OmpDirectiveKind directiveKind = OmpDirectiveKind.getHelper().fromValueTry(directiveKindString)
                .orElseThrow(() -> new RuntimeException("Can't set directive kind '" + directiveKindString
                        + "', name is not valid. Valid directive names: "
                        + OmpDirectiveKind.getHelper().getAvailableValues()));

        // Create new pragma based on the previous pragma
        OmpPragma newOmpPragma = OmpParser.newOmpPragma(directiveKind, this.getNodeImpl());

        // Replace previous pragma
        NodeInsertUtils.replace(this.getNodeImpl(), newOmpPragma);

        // Update join point pragma
        this.node = newOmpPragma;
    }
}
