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

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AIf;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.util.SpecsCollections;

public class CxxIf extends AIf {

    private final IfStmt ifStmt;

    public CxxIf(IfStmt ifStmt) {
        super(new CxxStatement(ifStmt));
        this.ifStmt = ifStmt;
    }

    @Override
    public ClavaNode getNode() {
        return ifStmt;
    }

    @Override
    public List<? extends AExpression> selectCond() {
        if (!(ifStmt.getCondition() instanceof Expr)) {
            return Collections.emptyList();
        }

        return Arrays.asList(CxxJoinpoints.create(ifStmt.getCondition(), AExpression.class));
    }

    @Override
    public List<? extends AScope> selectThen() {
        return ifStmt.getThen().map(then -> Arrays.asList(CxxJoinpoints.create(then, AScope.class)))
                .orElse(Collections.emptyList());
    }

    @Override
    public List<? extends AScope> selectElse() {
        return SpecsCollections.toStream(ifStmt.getElse())
                .map(stmt -> CxxJoinpoints.create(stmt, AScope.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends AScope> selectBody() {
        return selectThen();
    }

    @Override
    public List<? extends AVardecl> selectCondDecl() {
        return SpecsCollections.toList(ifStmt.getDeclCondition()
                .map(varDecl -> CxxJoinpoints.create(varDecl, AVardecl.class)));
        // if (!(ifStmt.getCondition() instanceof VarDecl)) {
        // return Collections.emptyList();
        // }
        //
        // return Arrays.asList(CxxJoinpoints.create((VarDecl) ifStmt.getCondition(), this, AVardecl.class));
    }

    @Override
    public AExpression getCondImpl() {
        return SpecsCollections.orElseNull(selectCond());
    }

    @Override
    public AJoinPoint getCondDeclImpl() {
        return SpecsCollections.orElseNull(selectCondDecl());
    }

    @Override
    public AScope getThenImpl() {
        return SpecsCollections.orElseNull(selectThen());
    }

    @Override
    public AScope getElseImpl() {
        return SpecsCollections.orElseNull(selectElse());
    }

    @Override
    public void defCondImpl(AExpression value) {
        ifStmt.setCondition((Expr) value.getNode());
    }

    @Override
    public void setCondImpl(AExpression cond) {
        defCondImpl(cond);
    }

    @Override
    public void defThenImpl(AStatement value) {
        ifStmt.setThen((Stmt) value.getNode());
    }

    @Override
    public void setThenImpl(AStatement then) {
        defThenImpl(then);
    }

    @Override
    public void defElseImpl(AStatement value) {
        ifStmt.setElse((Stmt) value.getNode());
    }

    @Override
    public void setElseImpl(AStatement _else) {
        defElseImpl(_else);
    }

}
