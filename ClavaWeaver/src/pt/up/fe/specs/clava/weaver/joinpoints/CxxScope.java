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

import pt.up.fe.specs.clang.clava.lara.LaraMarkerPragma;
import pt.up.fe.specs.clang.clava.lara.LaraTagPragma;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.comment.Comment;
import pt.up.fe.specs.clava.ast.omp.OmpPragma;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.LoopStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.weaver.CxxActions;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxSelects;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AComment;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AIf;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ALoop;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMarker;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AOmp;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.APragma;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ATag;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;

public class CxxScope extends AScope {

    private final CompoundStmt scope;
    private final ACxxWeaverJoinPoint parent;

    public CxxScope(CompoundStmt scope, ACxxWeaverJoinPoint parent) {
        super(new CxxStatement(scope, parent));
        // public CxxScope(CompoundStmt scope, ACxxWeaverJoinPoint parent) {
        this.scope = scope;
        this.parent = parent;
    }

    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        return parent;
    }

    @Override
    public ClavaNode getNode() {
        return scope;
    }

    /*
    @Override
    public void insertImpl(String position, String code) {
        // Special cases entry/exit
        String lowerCasePosition = position.toLowerCase();
        if (lowerCasePosition.equals("entry")) {
            insertEntryImpl(code);
            return;
        }
    
        if (lowerCasePosition.equals("exit")) {
            insertExitImpl(code);
            return;
        }
        super.insertImpl(position, code);
    }
    */
    @Override
    public void insertImpl(String position, String code) {
        // 'body' behaviour
        if (!scope.isNestedScope()) {
            Stmt literalStmt = ClavaNodeFactory.literalStmt(code);
            CxxActions.insertStmt(position, scope, literalStmt, getWeaverEngine());
            return;
        }

        // Default behaviour
        super.insertImpl(position, code);
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
        if (!scope.isNestedScope()) {
            // Transform, if needed, the given node into a stmt
            Stmt stmt = ClavaNodes.toStmt(node.getNode());
            return insertBodyImplJp("replace", stmt);
        }

        // Default behaviour
        return super.replaceWithImpl(node);
    }

    private AJoinPoint insertBodyImplJp(String position, ClavaNode newNode) {
        Stmt newStmt = CxxActions.getValidStatement(newNode);

        CxxActions.insertStmt(position, scope, newStmt, getWeaverEngine());

        // Body becomes the parent of this statement
        return CxxJoinpoints.create(newStmt, this);
    }

    @Override
    public AJoinPoint insertBeginImpl(String code) {
        return insertBeginImpl(AstFactory.stmtLiteral(code, this));
    }

    @Override
    public AJoinPoint insertBeginImpl(AJoinPoint node) {
        Stmt newStmt = ClavaNodes.toStmt(node.getNode());

        // Preconditions.checkArgument(node.getNode() instanceof Stmt,
        // "Expected input of action scope.insertEntry to be a Stmt joinpoint");

        CxxActions.insertStmt("before", scope, newStmt, getWeaverEngine());

        return node;
    }

    @Override
    public AJoinPoint insertEndImpl(String code) {
        return insertEndImpl(AstFactory.stmtLiteral(code, this));
    }

    @Override
    public AJoinPoint insertEndImpl(AJoinPoint node) {
        Stmt newStmt = ClavaNodes.toStmt(node.getNode());

        // Preconditions.checkArgument(newStmt instanceof Stmt,
        // "Expected input of action scope.insertEnd to be a Stmt joinpoint, is a " + node.getJoinPointType());
        CxxActions.insertStmt("after", scope, newStmt, getWeaverEngine());
        return node;
        /*
        List<? extends AStatement> statements = selectStatements();
        if (statements.isEmpty()) {
            throw new RuntimeException("Not yet implemented when scope is empty");
        }
        
        Stmt newStmt = CxxActions.getValidStatement(CollectionUtils.last(statements).getNode());
        
        insertImpl(position, newStmt);
        
        // Body becomes the parent of this statement
        return new CxxStatement(newStmt, this);
        */
    }

    @Override
    public Integer getNumStatementsImpl() {
        return getStatements().size();
    }

    private List<Stmt> getStatements() {
        return scope.toStatements();
    }

    @Override
    public List<? extends AStatement> selectStmt() {
        return CxxSelects.select(AStatement.class, getStatements(), true, this, CxxSelects::stmtFilter);

    }

    /*
    public static boolean stmtFilter(ClavaNode node) {
        if (!(node instanceof Stmt)) {
            return false;
        }
    
        Stmt stmt = (Stmt) node;
    
        if (stmt.isAggregateStmt()) {
            return false;
        }
    
        if (stmt instanceof LoopStmt || stmt instanceof IfStmt) {
            return false;
        }
    
        return true;
    }
    */

    @Override
    public List<? extends AStatement> selectChildStmt() {
        return getStatements().stream()
                .map(stmt -> (AStatement) CxxJoinpoints.create(stmt, this))
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends AScope> selectScope() {
        // It is a scope if the parent is a compound statement
        return CxxSelects.select(AScope.class, getStatements(), true, this,
                node -> node instanceof CompoundStmt && ((CompoundStmt) node).isNestedScope());

    }

    @Override
    public List<? extends AIf> selectIf() {
        return getStatements().stream()
                .filter(stmt -> stmt instanceof IfStmt)
                .map(stmt -> CxxJoinpoints.create((IfStmt) stmt, this, AIf.class))
                .collect(Collectors.toList());

    }

    @Override
    public List<? extends ALoop> selectLoop() {
        return getStatements().stream()
                .flatMap(stmt -> stmt.getDescendantsAndSelfStream())
                .filter(node -> node instanceof LoopStmt)
                .map(loop -> CxxJoinpoints.create((LoopStmt) loop, this, ALoop.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends APragma> selectPragma() {
        return CxxSelects.select(APragma.class, getStatements(), true, this, Pragma.class);
        /*
        return getStatements().stream()
                .flatMap(stmt -> stmt.getDescendantsAndSelfStream())
                .filter(node -> node instanceof Pragma)
                .map(pragma -> new CxxPragma((Pragma) pragma, this))
                .collect(Collectors.toList());
                */
    }

    @Override
    public List<? extends AMarker> selectMarker() {
        return CxxSelects.select(AMarker.class, getStatements(), true, this, LaraMarkerPragma.class);
    }

    @Override
    public List<? extends ATag> selectTag() {
        return CxxSelects.select(ATag.class, getStatements(), true, this, LaraTagPragma.class);
    }

    @Override
    public void clearImpl() {
        CxxActions.removeChildren(scope, getWeaverEngine());
    }

    @Override
    public List<? extends AOmp> selectOmp() {
        return CxxSelects.select(AOmp.class, getStatements(), true, this, OmpPragma.class);
    }

    @Override
    public List<? extends AComment> selectComment() {
        return CxxSelects.select(AComment.class, getStatements(), true, this, Comment.class::isInstance);
    }
}
