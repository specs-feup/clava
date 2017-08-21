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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.utils.StmtWithCondition;

public abstract class LoopStmt extends Stmt implements StmtWithCondition {

    private static final int DEFAULT_ITERATIONS = -1;

    public static int getDefaultIterations() {
        return DEFAULT_ITERATIONS;
    }

    private boolean isParallel;
    private int iterations;

    public LoopStmt(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);

        isParallel = false;
        iterations = DEFAULT_ITERATIONS;
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

    /*
    @Override
    public List<Stmt> toStatements() {
        return getBody().toStatements();
    }
    */

}
