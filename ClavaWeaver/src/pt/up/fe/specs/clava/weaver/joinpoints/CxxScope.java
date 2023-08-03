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

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.analysis.flow.control.ControlFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.ast.cilk.CilkFor;
import pt.up.fe.specs.clava.ast.cilk.CilkSync;
import pt.up.fe.specs.clava.ast.comment.Comment;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.lara.LaraMarkerPragma;
import pt.up.fe.specs.clava.ast.lara.LaraTagPragma;
import pt.up.fe.specs.clava.ast.omp.OmpPragma;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.LoopStmt;
import pt.up.fe.specs.clava.ast.stmt.ReturnStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.WrapperStmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxActions;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxSelects;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.Insert;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACilkFor;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACilkSync;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AComment;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AIf;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ALoop;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMarker;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AOmp;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.APragma;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AReturnStmt;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ATag;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.util.SpecsLogs;

public class CxxScope extends AScope {

    private final CompoundStmt scope;

    public CxxScope(CompoundStmt scope) {
        super(new CxxStatement(scope));
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
            Stmt literalStmt = CxxWeaver.getSnippetParser().parseStmt(code);
            CxxActions.insertStmt(position, scope, literalStmt, getWeaverEngine());
            return new AJoinPoint[] { CxxJoinpoints.create(literalStmt) };
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
        return CxxJoinpoints.create(newStmt);
    }

    @Override
    public AJoinPoint insertBeginImpl(String code) {
        return insertBeginImpl(AstFactory.stmtLiteral(code));
    }

    @Override
    public AJoinPoint insertBeginImpl(AJoinPoint node) {
        Stmt newStmt = ClavaNodes.toStmt(node.getNode());

        CxxActions.insertStmt("before", scope, newStmt, getWeaverEngine());

        return CxxJoinpoints.create(newStmt);
    }

    @Override
    public AJoinPoint insertEndImpl(String code) {
        return insertEndImpl(AstFactory.stmtLiteral(code));
    }

    @Override
    public AJoinPoint insertEndImpl(AJoinPoint node) {
        Stmt newStmt = ClavaNodes.toStmt(node.getNode());

        CxxActions.insertStmt("after", scope, newStmt, getWeaverEngine());

        return CxxJoinpoints.create(newStmt);
    }

    @Override
    public Long getNumStatementsImpl() {
        return numStatementsImpl(false);
    }

    @Override
    public Long numStatementsImpl(Boolean flat) {
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
    public List<? extends AStatement> selectStmt() {
        return CxxSelects.select(AStatement.class, getStatements(), true, CxxSelects::stmtFilter);
    }

    @Override
    public List<? extends AStatement> selectChildStmt() {
        return getStatements().stream().map(stmt -> (AStatement) CxxJoinpoints.create(stmt))
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends AScope> selectScope() {
        // It is a scope if the parent is a compound statement
        return CxxSelects.select(AScope.class, getStatements(), true,
                node -> node instanceof CompoundStmt && ((CompoundStmt) node).isNestedScope());

    }

    @Override
    public List<? extends AIf> selectIf() {
        return CxxSelects.select(AIf.class, getStatements(), true, IfStmt.class);
    }

    @Override
    public List<? extends ALoop> selectLoop() {
        return CxxSelects.select(ALoop.class, getStatements(), true, LoopStmt.class);
    }

    @Override
    public List<? extends APragma> selectPragma() {
        return CxxSelects.select(APragma.class, getStatements(), true, Pragma.class);
    }

    @Override
    public List<? extends AMarker> selectMarker() {
        return CxxSelects.select(AMarker.class, getStatements(), true, LaraMarkerPragma.class);
    }

    @Override
    public List<? extends ATag> selectTag() {
        return CxxSelects.select(ATag.class, getStatements(), true, LaraTagPragma.class);
    }

    @Override
    public void clearImpl() {
        CxxActions.removeChildren(scope, getWeaverEngine());
    }

    @Override
    public List<? extends AOmp> selectOmp() {
        return CxxSelects.select(AOmp.class, getStatements(), true, OmpPragma.class);
    }

    @Override
    public List<? extends AComment> selectComment() {
        return CxxSelects.select(AComment.class, getStatements(), true, Comment.class::isInstance);
    }

    @Override
    public Boolean getNakedImpl() {
        return scope.isNaked();
    }

    @Override
    public void defNakedImpl(Boolean value) {
        scope.setNaked(value);
    }

    @Override
    public void setNakedImpl(Boolean isNaked) {
        defNakedImpl(isNaked);
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

        AJoinPoint varDeclJp = CxxJoinpoints.create(varDecl);

        insertBegin(varDeclJp);

        return varDeclJp;
    }

    @Override
    public AStatement[] getStmtsArrayImpl() {
        return CxxJoinpoints.create(getNode().getChildren(Stmt.class), AStatement.class);
    }

    @Override
    public AStatement[] getAllStmtsArrayImpl() {
        return selectStmt().toArray(new AStatement[0]);
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
        return getAstParentImpl();
    }

    @Override
    public List<? extends AReturnStmt> selectReturnStmt() {
        return CxxSelects.select(AReturnStmt.class, getStatements(), true, ReturnStmt.class);
    }

    @Override
    public List<? extends ACilkFor> selectCilkFor() {
        return CxxSelects.select(ACilkFor.class, getStatements(), true, CilkFor.class);

    }

    @Override
    public List<? extends ACilkSync> selectCilkSync() {
        return CxxSelects.select(ACilkSync.class, getStatements(), true, CilkSync.class);

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
        return CxxActions.insertReturn(this, code);
    }

    @Override
    public AJoinPoint insertReturnImpl(String code) {
        var stmt = CxxJoinpoints.create(CxxWeaver.getSnippetParser().parseStmt(code));
        return insertReturnImpl(stmt);
    }
}
