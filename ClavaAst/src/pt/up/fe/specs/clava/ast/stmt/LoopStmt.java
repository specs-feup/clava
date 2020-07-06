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

package pt.up.fe.specs.clava.ast.stmt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.utils.StmtWithCondition;
import pt.up.fe.specs.symja.SymjaPlusUtils;
import pt.up.fe.specs.util.SpecsLogs;

public abstract class LoopStmt extends Stmt implements StmtWithCondition {

    // private static final Integer DEFAULT_ITERATIONS = null;

    public LoopStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

        isParallel = false;
        iterations = null;
        rank = null;
    }

    // public static int getDefaultIterations() {
    // return DEFAULT_ITERATIONS;
    // }

    private boolean isParallel;
    private Integer iterations;
    private List<Integer> rank;

    // public LoopStmt(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    // super(info, children);
    //
    // isParallel = false;
    // iterations = DEFAULT_ITERATIONS;
    // rank = null;
    // }

    public abstract CompoundStmt getBody();

    public CompoundStmt setBody(CompoundStmt newBody) {

        // Replace body statement
        CompoundStmt oldBody = getBody();
        int bodyIndex = oldBody.indexOfSelf();

        setChild(bodyIndex, newBody);

        return oldBody;
    }

    public boolean isParallel() {
        return isParallel;
    }

    public void setParallel(boolean isParallel) {
        this.isParallel = isParallel;
    }

    public Integer getIterations() {
        // If a custom number of iterations was set, it has priority
        if (iterations != null) {
            return iterations;
        }

        // Simplify iterations expression
        String simplifiedIterations = getIterationsExpr().map(expr -> SymjaPlusUtils.simplify(expr.getCode()))
                .orElse(null);

        if (simplifiedIterations == null) {
            return null;
        }

        // Try to parse as number
        try {
            return new BigDecimal(simplifiedIterations).intValue();
        } catch (Exception e) {
            SpecsLogs.debug(() -> "LoopStmt.getIterations(): could not convert iterations '" + simplifiedIterations
                    + "' to a number ");
            return null;
        }
    }

    /**
     * By default, returns empty.
     * 
     * @return
     */
    public Optional<Expr> getIterationsExpr() {
        return Optional.empty();
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public List<Integer> getRank() {
        // Calculate rank if it has not been initialized yet
        if (rank == null) {
            rank = calculateRank();
        }

        return rank;
    }

    private List<Integer> calculateRank() {

        // Get own rank number
        int ownRank = calculateOwnRank();

        // Get rank of parent loop
        List<Integer> parentRank = getAncestorTry(LoopStmt.class).map(LoopStmt::getRank)
                .orElse(Collections.emptyList());

        List<Integer> newRank = new ArrayList<>(parentRank.size() + 1);
        newRank.addAll(parentRank);
        newRank.add(ownRank);

        return newRank;
    }

    private int calculateOwnRank() {

        // Get ancestor node relative to the rank
        ClavaNode ancestorRankNode = getAncestorRankNode();

        // Create list of rank siblings.
        List<LoopStmt> rankSiblings = buildRankSiblings(ancestorRankNode);

        // Return index of own node
        int indexOfLoop = rankSiblings.indexOf(this);

        Preconditions.checkArgument(indexOfLoop != -1, "Could not find itself inside its loop rank siblings");

        // Loop ranks start at 1
        return indexOfLoop + 1;
        /*
        // Calculate own rank
        ClavaNode parentNode = getParent();
        int currentRank = 1;
        for (ClavaNode sibling : parentNode.getChildren()) {
        
            // If found itself, return
            if (sibling == this) {
                return currentRank;
            }
        
            // If found loop that is not itself, increase rank
            if (sibling instanceof LoopStmt) {
                currentRank++;
            }
        }
        
        throw new RuntimeException("Could not find itself inside of parent's children");
        */
    }

    private List<LoopStmt> buildRankSiblings(ClavaNode ancestorRankNode) {
        List<LoopStmt> rankSiblings = new ArrayList<>();

        for (ClavaNode child : ancestorRankNode.getChildren()) {
            buildRankSiblingsPrivate(child, rankSiblings);
        }

        return rankSiblings;
    }

    private void buildRankSiblingsPrivate(ClavaNode node, List<LoopStmt> rankSiblings) {
        // If not a statement, stop looking
        if (!(node instanceof Stmt)) {
            return;
        }

        // If LoopStmt, add to list and stop looking
        if (node instanceof LoopStmt) {
            rankSiblings.add((LoopStmt) node);
            return;
        }

        // Continue looking in the children of the stmt
        node.getChildrenStream().forEach(child -> buildRankSiblingsPrivate(child, rankSiblings));
        /*
        // If an aggregate statement, continue looking in its children
        if (node instanceof Stmt && ((Stmt) node).isAggregateStmt()) {
            node.getChildrenStream().forEach(child -> buildRankSiblingsPrivate(child, rankSiblings));
            return;
        }
        
        // For all other kinds of nodes, stop looking
        return;
        */
    }

    private ClavaNode getAncestorRankNode() {
        // Get first ancestor that is a LoopStmt.
        ClavaNode loopAncestor = getAncestorTry(LoopStmt.class).orElse(null);
        if (loopAncestor != null) {
            return loopAncestor;
        }

        // If no LoopStmt found, use the first Decl ancestor as ancestor node
        return getAncestor(Decl.class);
    }

    /**
     * Uniquely identifies the loop in the code.
     *
     * <p>
     * Currently uses the loop file, function and rank to identify the loop
     */
    public String getLoopId() {
        // String fileId = "file$"
        // + getAncestorTry(TranslationUnit.class)
        // .map(tunit -> tunit.getRelativeFilepath())
        // .orElse("<no_file>");
        //
        // String functionId = "function$" + getAncestorTry(FunctionDecl.class)
        // .map(functionDecl -> functionDecl.getDeclarationId(false))
        // .orElse("<no_function>");

        // Get function id
        String functionId = getAncestorTry(FunctionDecl.class)
                .map(functionDecl -> functionDecl.getNodeId())
                .orElse("<no_function>");

        String rankId = "rank$" + getRank().stream()
                .map(rankValue -> rankValue.toString())
                .collect(Collectors.joining("."));

        return functionId + getNodeIdSeparator() + rankId;
        // return fileId + "->" + functionId + "->" + rankId;
    }

    @Override
    public String getNodeId() {
        return getLoopId();
    }

    /*
    @Override
    public List<Stmt> toStatements() {
        return getBody().toStatements();
    }
    */

}
