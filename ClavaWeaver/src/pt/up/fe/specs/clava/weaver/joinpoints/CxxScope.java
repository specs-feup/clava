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
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.util.SpecsLogs;

public class CxxScope extends AScope {

    private final CompoundStmt scope;

    public CxxScope(CompoundStmt scope, CxxWeaver weaver) {
        super(new CxxStatement(scope, weaver), weaver);
        this.scope = scope;
    }

    @Override
    public ClavaNode getNode() {
        return scope;
    }

    @Override
    public AJoinPoint[] insertImpl(String position, String code) {

        // 'body' behaviour
        if (!scope.isNestedScope()) {
            Stmt literalStmt = getWeaverEngine().getSnippetParser().parseStmt(code);
            CxxActions.insertStmt(position, scope, literalStmt, getWeaverEngine());
            return new AJoinPoint[] { CxxJoinpoints.create(literalStmt, getWeaverEngine()) };
        }

        // Default behaviour
        return super.insertImpl(position, code);
    }

    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {

        // 'body' behaviour
        if (!scope.isNestedScope()) {
            ClavaLog.warning("Avoid using action 'insert before' over 'body' joinpoint, use 'insertBegin' instead.");
            return insertBodyImplJp("before", node.getNode());
        }

        return super.insertBeforeImpl(node);
    }

    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {

        // 'body' behaviour
        if (!scope.isNestedScope()) {
            ClavaLog.warning("Avoid using action 'insert after' over 'body' joinpoint, use 'insertEnd' instead.");
            return insertBodyImplJp("after", node.getNode());
        }

        return super.insertAfterImpl(node);
    }

    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {

        // 'body' behaviour
        if (!scope.isNestedScope() && !(node instanceof AScope)) {

            // Transform, if needed, the given node into a stmt
            Stmt stmt = ClavaNodes.toStmt(node.getNode());
            return insertBodyImplJp("replace", stmt);
        }

        // Default behaviour
        return super.replaceWithImpl(node);
    }

    private AJoinPoint insertBodyImplJp(String position, ClavaNode newNode) {

        Stmt newStmt = ClavaNodes.getValidStatement(newNode, Insert.valueOf(position.toUpperCase()).toPosition());
        if (newStmt == null) {
            return null;
        }

        CxxActions.insertStmt(position, scope, newStmt, getWeaverEngine());

        // Body becomes the parent of this statement
        return CxxJoinpoints.create(newStmt, getWeaverEngine());
    }

    @Override
    public AJoinPoint insertBeginImpl(String code) {
        return insertBeginImpl(AstFactory.stmtLiteral(getWeaverEngine(), code));
    }

    @Override
    public AJoinPoint insertBeginImpl(AJoinPoint node) {
        Stmt newStmt = ClavaNodes.toStmt(node.getNode());

        CxxActions.insertStmt("before", scope, newStmt, getWeaverEngine());

        return CxxJoinpoints.create(newStmt, getWeaverEngine());
    }

    @Override
    public AJoinPoint insertEndImpl(String code) {
        return insertEndImpl(AstFactory.stmtLiteral(getWeaverEngine(), code));
    }

    @Override
    public AJoinPoint insertEndImpl(AJoinPoint node) {
        Stmt newStmt = ClavaNodes.toStmt(node.getNode());

        CxxActions.insertStmt("after", scope, newStmt, getWeaverEngine());

        return CxxJoinpoints.create(newStmt, getWeaverEngine());
    }

    @Override
    public Long getNumStatementsImpl(Boolean flat) {
        var nodesStream = flat ? scope.getChildrenStream() : scope.getDescendantsStream();

        return nodesStream.filter(Stmt.class::isInstance)
                // Ignore CompoundStmt, etc
                .filter(stmt -> !((Stmt) stmt).isAggregateStmt())
                // Ignore comments, pragmas
                .filter(stmt -> !(stmt instanceof WrapperStmt)).count();
    }

    private List<Stmt> getStatements() {
        return scope.toStatements();
    }

    @Override
    public void clearImpl() {
        CxxActions.removeChildren(scope, getWeaverEngine());
    }

    @Override
    public Boolean getNakedImpl() {
        return scope.isNaked();
    }

    @Override
    public void setNakedImpl(Boolean isNaked) {
        scope.setNaked(isNaked);
    }

    @Override
    public AJoinPoint addLocalImpl(String name, AJoinPoint type, String initValue) {

        // Check if joinpoint is a CxxType
        if (!(type instanceof AType)) {
            SpecsLogs.msgInfo("addLocal: the provided join point (" + type.getJoinPointType() + ") is not a type");
            return null;
        }

        Type typeNode = (Type) type.getNode();

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

        AJoinPoint varDeclJp = CxxJoinpoints.create(varDecl, getWeaverEngine());

        insertBegin(varDeclJp);

        return varDeclJp;
    }

    @Override
    public AStatement[] getStmtsArrayImpl() {
        return CxxJoinpoints.create(getNode().getChildren(Stmt.class), getWeaverEngine(), AStatement.class);
    }

    @Override
    public AStatement[] getAllStmtsArrayImpl() {
        return CxxSelects.select(getWeaverEngine(), AStatement.class, getStatements(), true, CxxSelects::stmtFilter).toArray(new AStatement[0]);
    }

    @Override
    public AStatement getFirstStmtImpl() {
        AStatement[] stmts = getStmtsArrayImpl();

        if (stmts.length == 0) {
            return null;
        }

        return stmts[0];

    }

    @Override
    public AStatement getLastStmtImpl() {
        AStatement[] stmts = getStmtsArrayImpl();

        if (stmts.length == 0) {
            return null;
        }

        return stmts[stmts.length - 1];
    }

    @Override
    public AJoinPoint getOwnerImpl() {
        // TODO: This should generically work, but corner cases have not been checked
        return getParentImpl();
    }

    @Override
    public String cfgImpl() {
        ControlFlowGraph cfg = new ControlFlowGraph(scope);
        var cfgDot = cfg.toDot();
        ClavaLog.info(cfgDot);
        return cfgDot;
    }

    @Override
    public String dfgImpl() {
        DataFlowGraph dfg = new DataFlowGraph(scope);
        var dfgDot = dfg.toDot();
        ClavaLog.info(dfgDot);
        return dfgDot;
    }

    @Override
    public AJoinPoint insertReturnImpl(AJoinPoint code) {
        return CxxActions.insertReturn(this, code, getWeaverEngine());
    }

    @Override
    public AJoinPoint insertReturnImpl(String code) {
        var stmt = CxxJoinpoints.create(getWeaverEngine().getSnippetParser().parseStmt(code), getWeaverEngine());
        return insertReturnImpl(stmt);
    }
}
