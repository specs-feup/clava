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

package pt.up.fe.specs.clava.transform.loop;

import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.LoopStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

public class LoopInterchange {

    private final ForStmt outLoop;
    private final ForStmt inLoop;

    public static Optional<LoopInterchange> newInstance(LoopStmt loop1, LoopStmt loop2) {

        ForStmt outerLoop = testLoops(loop1, loop2);

        if (outerLoop == null) {

            return Optional.empty();
        }

        ForStmt innerLoop = (ForStmt) (outerLoop != loop1 ? loop1 : loop2);

        LoopInterchange loopInterchage = new LoopInterchange(outerLoop, innerLoop);

        return Optional.of(loopInterchage);

    }

    private static LoopStmt getOuterLoop(LoopStmt loop1, LoopStmt loop2) {

        if (loop1.isAncestor(loop2)) {
            return loop2;
        }

        if (loop2.isAncestor(loop1)) {
            return loop1;
        }

        return null;
    }

    private LoopInterchange(ForStmt outsideLoop, ForStmt insideLoop) {
        outLoop = outsideLoop;
        inLoop = insideLoop;
    }

    public static boolean test(LoopStmt loop1, LoopStmt loop2) {
        return testLoops(loop1, loop2) != null;
    }

    /**
     * Verify if loops are:<br>
     * <li>for loops
     * <li>nested with each other
     * <li>well behaved ({@link LoopAnalysisUtils#hasHeader(ForStmt)}
     *
     * @param loop1
     *            the first loop
     * @param loop2
     *            the second loop
     * @return The outermost loop, or null if the verifications fail
     */
    private static ForStmt testLoops(LoopStmt loop1, LoopStmt loop2) {
        /* must be for loops  */
        if (!(loop1 instanceof ForStmt)) {
            return null;
            // return false;
        }

        if (!(loop2 instanceof ForStmt)) {
            return null;
        }

        ForStmt outerLoop = (ForStmt) getOuterLoop(loop1, loop2);
        if (outerLoop == null) {
            return null;
        }

        if (loop1.getBody().getStatements().isEmpty()) {
            return null;
        }

        if (loop2.getBody().getStatements().isEmpty()) {
            return null;
        }

        ForStmt innerLoop = (ForStmt) (outerLoop != loop1 ? loop1 : loop2);

        boolean testBehaviour = testBehaviour(outerLoop, innerLoop);
        return testBehaviour ? outerLoop : null;
    }

    private static boolean testBehaviour(ForStmt outLoop, ForStmt inLoop) {

        /* checks if loops have init, cond and inc */
        if (!(LoopAnalysisUtils.hasHeader(outLoop) && LoopAnalysisUtils.hasHeader(inLoop))) {

            return false;
        }

        /* checks if loops are perfectly nested */
        if (!inLoop.isAncestor(outLoop)) {
            return false;
        }

        // Optional<CompoundStmt> body = outLoop.getBody();
        CompoundStmt body = outLoop.getBody();
        // Stmt firstStmt = body.map(stmts -> stmts.getStatements().get(0)).orElse(null);
        Stmt firstStmt = body.getStatements().get(0);

        while (firstStmt != null) {

            if (firstStmt == inLoop) {

                return true;
            }

            if (!(firstStmt instanceof ForStmt)) {
                return false;
            }

            // firstStmt = ((ForStmt) firstStmt).getBody().map(stmts -> stmts.getStatements().get(0)).orElse(null);
            firstStmt = ((ForStmt) firstStmt).getBody().getStatements().get(0);
        }

        /* the loops are not nested out -> inner */
        return false;
    }

    public void apply() {

        TransformQueue<ClavaNode> queue = new TransformQueue<>("loop_interchange_queue");

        ForStmt outForLoop = outLoop;
        ForStmt inForLoop = inLoop;

        queue.swap(outForLoop.getInit().get(), inForLoop.getInit().get());
        queue.swap(outForLoop.getCond().get(), inForLoop.getCond().get());
        queue.swap(outForLoop.getInc().get(), inForLoop.getInc().get());

        queue.apply();
    }
}
