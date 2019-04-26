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

package pt.up.fe.specs.clang.transforms;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.SwitchCase;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Replaces statement in CaseStmt and DefaultStmt with a naked CompoundStmt, which contains all the statements in the
 * case, until the next CaseStmt or DefaultStmt.
 * 
 * @author JoaoBispo
 *
 */
public class NormalizeCases implements SimplePreClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        if (!(node instanceof SwitchCase)) {
            return;
        }

        SwitchCase switchCase = (SwitchCase) node;

        List<Stmt> subStmts = switchCase.getSubStmts();

        // Before normalization, each Case only has one statement
        SpecsCheck.checkSize(subStmts, 1);
        Stmt subStmt = subStmts.get(0);

        // If statement is a CaseStmt/DefaultStmt, move instruction to after Case
        if (subStmt instanceof SwitchCase) {
            queue.moveAfter(node, subStmt);
            return;
        }

        // Build list of instruction next to the case that are not a SwitchCase
        List<ClavaNode> switchCaseSiblings = switchCase.getAncestor(CompoundStmt.class).getChildren();

        // If parent is a SwitchCase, needs to get the siblings of the corresponding ancestor case that
        // is closest to the CompountStmt
        int caseIndex = getCaseIndex(switchCase);

        List<ClavaNode> switchCaseSubStmts = new ArrayList<>();

        for (int i = caseIndex + 1; i < switchCaseSiblings.size(); i++) {
            ClavaNode sibling = switchCaseSiblings.get(i);
            // If a switch case is found, stop
            if (sibling instanceof SwitchCase) {
                break;
            }

            // Otherwise, add sibling
            switchCaseSubStmts.add(sibling);
        }

        // Move instructions to be children of the case
        SpecsCollections.reverseStream(switchCaseSubStmts)
                .forEach(newSubStmt -> queue.moveAfter(subStmt, newSubStmt));

    }

    private int getCaseIndex(SwitchCase switchCase) {
        ClavaNode caseParent = switchCase.getParent();
        if (!(caseParent instanceof SwitchCase)) {
            return switchCase.indexOfSelf();
        }

        ClavaNode currentSwitchCase = caseParent;
        while (currentSwitchCase.getParent() instanceof SwitchCase) {
            currentSwitchCase = currentSwitchCase.getParent();
        }

        return currentSwitchCase.indexOfSelf();
    }

}
