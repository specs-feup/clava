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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.List;

import org.lara.interpreter.weaver.interf.enums.InsertPosition;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.analysis.flow.control.ControlFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.WrapperStmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxActions;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxSelects;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.Insert;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinpoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.util.SpecsLogs;

public class CxxScope<Self extends CxxScope<Self>> extends AScope<Self> {

    public CxxScope(CompoundStmt scope, CxxWeaver weaver) {
        super(scope, weaver);
    }

    @Override
    public CompoundStmt getNodeImpl() {
        return (CompoundStmt) super.getNodeImpl();
    }

    @Override
    public AJoinpoint<?>[] insertImpl(InsertPosition position, String code) {

        // 'body' behaviour
        if (!this.getNodeImpl().isNestedScope()) {
            Stmt literalStmt = getWeaverEngine().getSnippetParser().parseStmt(code);
            CxxActions.insertStmt(position, this.getNodeImpl(), literalStmt, getWeaverEngine());
            return new AJoinpoint[] { CxxJoinpoints.create(literalStmt, getWeaverEngine()) };
        }

        // Default behaviour
        return super.insertImpl(position, code);
    }

    @Override
    public AJoinpoint<?> insertBeforeImpl(AJoinpoint<?> node) {

        // 'body' behaviour
        if (!this.getNodeImpl().isNestedScope()) {
            ClavaLog.warning("Avoid using action 'insert before' over 'body' joinpoint, use 'insertBegin' instead.");
            return insertBodyImplJp(InsertPosition.BEFORE, node.getNodeImpl());
        }

        return super.insertBeforeImpl(node);
    }

    @Override
    public AJoinpoint<?> insertAfterImpl(AJoinpoint<?> node) {

        // 'body' behaviour
        if (!this.getNodeImpl().isNestedScope()) {
            ClavaLog.warning("Avoid using action 'insert after' over 'body' joinpoint, use 'insertEnd' instead.");
            return insertBodyImplJp(InsertPosition.AFTER, node.getNodeImpl());
        }

        return super.insertAfterImpl(node);
    }

    @Override
    public AJoinpoint<?> replaceWithImpl(AJoinpoint<?> node) {

        // 'body' behaviour
        if (!this.getNodeImpl().isNestedScope() && !(node instanceof AScope)) {

            // Transform, if needed, the given node into a stmt
            Stmt stmt = ClavaNodes.toStmt(node.getNodeImpl());
            return insertBodyImplJp(InsertPosition.REPLACE, stmt);
        }

        // Default behaviour
        return super.replaceWithImpl(node);
    }

    private AJoinpoint<?> insertBodyImplJp(InsertPosition position, ClavaNode newNode) {

        Stmt newStmt = ClavaNodes.getValidStatement(newNode, Insert.valueOf(position.getDisplay().toUpperCase()).toPosition());
        if (newStmt == null) {
            return null;
        }

        CxxActions.insertStmt(position, this.getNodeImpl(), newStmt, getWeaverEngine());

        // Body becomes the parent of this statement
        return CxxJoinpoints.create(newStmt, getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> insertBeginImpl(String code) {
        return insertBeginImpl(AstFactory.stmtLiteral(getWeaverEngine(), code));
    }

    @Override
    public AJoinpoint<?> insertBeginImpl(AJoinpoint<?> node) {
        Stmt newStmt = ClavaNodes.toStmt(node.getNodeImpl());

        CxxActions.insertStmt(InsertPosition.BEFORE, this.getNodeImpl(), newStmt, getWeaverEngine());

        return CxxJoinpoints.create(newStmt, getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> insertEndImpl(String code) {
        return insertEndImpl(AstFactory.stmtLiteral(getWeaverEngine(), code));
    }

    @Override
    public AJoinpoint<?> insertEndImpl(AJoinpoint<?> node) {
        Stmt newStmt = ClavaNodes.toStmt(node.getNodeImpl());

        CxxActions.insertStmt(InsertPosition.AFTER, this.getNodeImpl(), newStmt, getWeaverEngine());

        return CxxJoinpoints.create(newStmt, getWeaverEngine());
    }

    @Override
    public long getGetNumStatementsImpl(boolean flat) {
        var nodesStream = flat ? this.getNodeImpl().getChildrenStream() : this.getNodeImpl().getDescendantsStream();

        return nodesStream.filter(Stmt.class::isInstance)
                // Ignore CompoundStmt, etc
                .filter(stmt -> !((Stmt) stmt).isAggregateStmt())
                // Ignore comments, pragmas
                .filter(stmt -> !(stmt instanceof WrapperStmt)).count();
    }

    private List<Stmt> getStatements() {
        return this.getNodeImpl().toStatements();
    }

    @Override
    public void clearImpl() {
        CxxActions.removeChildren(this.getNodeImpl(), getWeaverEngine());
    }

    @Override
    public boolean getNakedImpl() {
        return this.getNodeImpl().isNaked();
    }

    @Override
    public void setNakedImpl(boolean isNaked) {
        this.getNodeImpl().setNaked(isNaked);
    }

    @Override
    public AJoinpoint<?> addLocalImpl(String name, AJoinpoint<?> type, String initValue) {

        // Check if joinpoint is a CxxType
        if (!(type instanceof AType)) {
            SpecsLogs.msgInfo("addLocal: the provided join point (" + type.joinPointType() + ") is not a type");
            return null;
        }

        Type typeNode = (Type) type.getNodeImpl();

        // defaults as no init
        Expr initExpr = null;

        if (initValue != null) {
            initExpr = getFactory().literalExpr(initValue, getFactory().nullType());
        }

        VarDecl varDecl = getFactory().varDecl(name, typeNode);
        if (initExpr != null) {
            varDecl.setInit(initExpr);
        }
        varDecl.set(VarDecl.IS_USED);

        AJoinpoint<?> varDeclJp = CxxJoinpoints.create(varDecl, getWeaverEngine());

        insertBegin(varDeclJp);

        return varDeclJp;
    }

    @Override
    public AStatement<?>[] getStmtsImpl() {
        return CxxJoinpoints.create(getNodeImpl().getChildren(Stmt.class), getWeaverEngine(), AStatement.class);
    }

    @Override
    public AStatement<?>[] getAllStmtsImpl() {
        return CxxSelects.select(getWeaverEngine(), AStatement.class, getStatements(), true, CxxSelects::stmtFilter);
    }

    @Override
    public AStatement<?> getFirstStmtImpl() {
        AStatement<?>[] stmts = getStmtsImpl();

        if (stmts.length == 0) {
            return null;
        }

        return stmts[0];

    }

    @Override
    public AStatement<?> getLastStmtImpl() {
        AStatement<?>[] stmts = getStmtsImpl();

        if (stmts.length == 0) {
            return null;
        }

        return stmts[stmts.length - 1];
    }

    @Override
    public AJoinpoint<?> getOwnerImpl() {
        // TODO: This should generically work, but corner cases have not been checked
        return getParentImpl();
    }

    @Override
    public String cfgImpl() {
        ControlFlowGraph cfg = new ControlFlowGraph(this.getNodeImpl());
        var cfgDot = cfg.toDot();
        ClavaLog.info(cfgDot);
        return cfgDot;
    }

    @Override
    public String dfgImpl() {
        DataFlowGraph dfg = new DataFlowGraph(this.getNodeImpl());
        var dfgDot = dfg.toDot();
        ClavaLog.info(dfgDot);
        return dfgDot;
    }

    @Override
    public AJoinpoint<?> insertReturnImpl(AJoinpoint<?> code) {
        return CxxActions.insertReturn(this, code, getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> insertReturnImpl(String code) {
        var stmt = CxxJoinpoints.create(getWeaverEngine().getSnippetParser().parseStmt(code), getWeaverEngine());
        return insertReturnImpl(stmt);
    }
}
