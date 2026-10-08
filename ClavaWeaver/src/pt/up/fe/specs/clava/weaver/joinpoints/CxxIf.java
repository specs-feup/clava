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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AIf;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.util.SpecsCollections;

public class CxxIf<Self extends CxxIf<Self>> extends AIf<Self> {

    public CxxIf(IfStmt ifStmt, CxxWeaver weaver) {
        super(ifStmt, weaver);
    }

    @Override
    public IfStmt getNodeImpl() {
        return (IfStmt) super.getNodeImpl();
    }

    @Override
    public AExpression<?> getCondImpl() {
        List<AExpression<?>> list = Collections.emptyList();

        if ((this.getNodeImpl().getCondition() instanceof Expr)) {
            list = Arrays.asList(CxxJoinpoints.create(this.getNodeImpl().getCondition(), getWeaverEngine(), AExpression.class));
        }

        return SpecsCollections.orElseNull(list);
    }

    @Override
    public AVardecl<?> getCondDeclImpl() {
        return SpecsCollections.orElseNull(SpecsCollections.toList(this.getNodeImpl().getDeclCondition()
                .map(varDecl -> CxxJoinpoints.create(varDecl, getWeaverEngine(), AVardecl.class))));
    }

    @Override
    public AScope<?> getThenImpl() {
        return SpecsCollections.orElseNull(
                this.getNodeImpl().getThen().map(then -> Arrays.asList(CxxJoinpoints.create(then,
                        getWeaverEngine(), AScope.class)))
                        .orElse(Collections.emptyList()));
    }

    @Override
    public AScope<?> getElseImpl() {
        return SpecsCollections.orElseNull(SpecsCollections.toStream(this.getNodeImpl().getElse())
                .map(stmt -> CxxJoinpoints.create(stmt,
                        getWeaverEngine(), AScope.class))
                .collect(Collectors.toList()));
    }

    @Override
    public void setCondImpl(AExpression<?> cond) {
        this.getNodeImpl().setCondition((Expr) cond.getNodeImpl());
    }

    @Override
    public void setThenImpl(AStatement<?> then) {
        this.getNodeImpl().setThen((Stmt) then.getNodeImpl());
    }

    @Override
    public void setElseImpl(AStatement<?> _else) {
        this.getNodeImpl().setElse((Stmt) _else.getNodeImpl());
    }

}
