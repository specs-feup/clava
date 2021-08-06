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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * Represents statements.
 * 
 * @author JoaoBispo
 *
 */
public abstract class Stmt extends ClavaNode {

    public Stmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public Stmt copy() {
        return (Stmt) super.copy();
    }

    /**
     * Default implementation returns a list with itself. However, in some cases (e.g., CompoundStmt, CapturedStmt), a
     * Stmt can contain several statements.
     * 
     * @return
     */
    public List<Stmt> toStatements() {
        return Arrays.asList(this);
    }

    /**
     * An aggregate statement is a statement that is just use to keep together other statements (e.g., CompoundStmt,
     * CapturedStmt...).
     * 
     * <p>
     * By default, returns false.
     * 
     * @return
     */
    public boolean isAggregateStmt() {
        return false;
    }

}
