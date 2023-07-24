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

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * Represents a return statement.
 * 
 * @author JoaoBispo
 *
 */
public class BreakStmt extends Stmt {

    public BreakStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        return "break;";
    }

    /**
     * 
     * @return the first ancestor that is either a switch or a loop, or throws exception if none is found
     */
    public Stmt getEnclosingStmt() {
        var currentAncestor = getParent();
        while (currentAncestor != null) {
            if (currentAncestor instanceof LoopStmt || currentAncestor instanceof SwitchStmt) {
                return (Stmt) currentAncestor;
            }

            currentAncestor = currentAncestor.getParent();
        }

        throw new RuntimeException("Expected to find either a loop or a switch as an ancestor");
    }

}
