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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.utils.StmtWithCondition;

public abstract class LoopStmt extends Stmt implements StmtWithCondition {

    private static final int DEFAULT_ITERATIONS = -1;

    public static int getDefaultIterations() {
        return DEFAULT_ITERATIONS;
    }

    private boolean isParallel;
    private int iterations;
    private List<Integer> rank;

    public LoopStmt(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);

        isParallel = false;
        iterations = DEFAULT_ITERATIONS;
        rank = null;
    }

    public abstract CompoundStmt getBody();

    public boolean isParallel() {
        return isParallel;
    }

    public void setParallel(boolean isParallel) {
        this.isParallel = isParallel;
    }

    public int getIterations() {
        return iterations;
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
    }

    /**
     * Uniquely identifies the loop in the code.
     * 
     * <p>
     * Currently uses the loop file, function and rank to identify the loop
     */
    public String getLoopId() {
        String fileId = "file$"
                + getAncestorTry(TranslationUnit.class)
                        .map(TranslationUnit::getFilepath)
                        .orElse("<no_file>");

        String functionId = "function$" + getAncestorTry(FunctionDecl.class)
                .map(functionDecl -> functionDecl.getDeclarationId(false))
                .orElse("<no_function>");

        String rankId = "rank$" + getRank().stream()
                .map(rankValue -> rankValue.toString())
                .collect(Collectors.joining("."));

        return fileId + "->" + functionId + "->" + rankId;
    }

    /*
    @Override
    public List<Stmt> toStatements() {
        return getBody().toStatements();
    }
    */

}
