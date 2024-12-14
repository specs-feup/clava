/**
 * Copyright 2016 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.utils;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;

import java.util.Optional;

public interface StmtWithCondition {

    /**
     * Because ifs and whiles can have a VarDecl as a condition (instead of expression), this method returns ClavaNode,
     * the common node between both.
     *
     * @return the node of this statement that represents a condition
     */
    Optional<ClavaNode> getStmtCondition();

    Optional<Expr> getStmtCondExpr();
}
