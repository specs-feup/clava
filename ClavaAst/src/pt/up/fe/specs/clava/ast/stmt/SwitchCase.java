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
import java.util.List;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Represents a switch case.
 * 
 * @author JoaoBispo
 *
 */
public abstract class SwitchCase extends Stmt {

    public SwitchCase(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Optional<Stmt> getSubStmt() {
        return ClavaNodes.nextNode(this).map(node -> (Stmt) node);
    }

    public boolean isEmptyCase() {
        var nextStmt = getSubStmt().orElse(null);
        SpecsCheck.checkNotNull(nextStmt, () -> "Case has no sub statement, is this correct?");

        return nextStmt instanceof SwitchCase;
    }

    /**
     * 
     * @return the next instruction that is not a case statement, or null if none is found (is it possible to return
     *         null?)
     */
    public Stmt nextExecutedInstruction() {
        var nextNode = ClavaNodes.nextNode(this).orElse(null);
        while (nextNode instanceof SwitchCase) {
            nextNode = ClavaNodes.nextNode(nextNode).orElse(null);
        }

        return (Stmt) nextNode;
    }

    /**
     * 
     * @return the instructions that are associated with this case in the source code. This does not represent what
     *         instructions are actually executed (e.g., if a case does not have a break, does not show instructions of
     *         the next case)
     */
    public List<Stmt> getInstructions() {
        var instructions = new ArrayList<Stmt>();
        var nextNode = ClavaNodes.nextNode(this).orElse(null);

        while (nextNode != null && !(nextNode instanceof SwitchCase)) {
            instructions.add((Stmt) nextNode);
            nextNode = ClavaNodes.nextNode(nextNode).orElse(null);
        }

        return instructions;
    }

    public abstract boolean isDefaultCase();
}
