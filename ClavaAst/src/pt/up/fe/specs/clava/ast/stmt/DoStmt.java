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

package pt.up.fe.specs.clava.ast.stmt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.Expr;

public class DoStmt extends LoopStmt {

    public DoStmt(ClavaNodeInfo info, CompoundStmt body, Expr condition) {
        this(info, Arrays.asList(body, condition));
    }

    private DoStmt(ClavaNodeInfo info, List<? extends ClavaNode> children) {
        super(info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new DoStmt(getInfo(), Collections.emptyList());
    }

    @Override
    public Optional<ClavaNode> getStmtCondition() {
        return Optional.of(getCondition());
    }

    public Expr getCondition() {
        return getChild(Expr.class, 1);
    }

    @Override
    public CompoundStmt getBody() {
        return getChild(CompoundStmt.class, 0);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append("do ").append(getBody().getCode()).append("while (").append(getCondition().getCode()).append(");");

        return code.toString();

    }

}
