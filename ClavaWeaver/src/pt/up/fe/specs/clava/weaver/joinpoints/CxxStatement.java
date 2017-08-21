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
import java.util.Optional;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.DeclaratorDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.ArraySubscriptExpr;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxSelects;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AArrayAccess;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ABinaryOp;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACall;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AUnaryOp;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVarref;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class CxxStatement extends AStatement {

    private final Stmt stmt;
    private final ACxxWeaverJoinPoint parent;

    public CxxStatement(Stmt stmt, ACxxWeaverJoinPoint parent) {
        this.stmt = stmt;
        this.parent = parent;
    }

    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        return parent;
    }

    @Override
    public ClavaNode getNode() {
        return stmt;
    }

    /*
    @Override
    public List<? extends ALoop> selectLoop() {
    return stmt.getDescendantsAndSelfStream()
    	.filter(node -> node instanceof LoopStmt)
    	.map(loop -> new CxxLoop((LoopStmt) loop, this))
    	.collect(Collectors.toList());
    }
    
    @Override
    public List<? extends ACall> selectCall() {
    return stmt.getDescendantsStream()
    	.filter(node -> node instanceof CallExpr)
    	.map(loop -> new CxxCall((CallExpr) loop, this))
    	.collect(Collectors.toList());
    }
    */

    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        // First "transform" node to insert into a statement
        Stmt newStmt = ClavaNodes.toStmt(node.getNode());

        NodeInsertUtils.replace(stmt, newStmt);

        // Return a statement joinpoint
        return CxxJoinpoints.create(newStmt, null);
    }

    @Override
    public List<? extends ACall> selectStmtCall() {
        List<Stmt> statements = stmt.toStatements();

        if (statements.size() != 1) {
            return Collections.emptyList();
        }

        Stmt statement = statements.get(0);

        if (!(statement instanceof ExprStmt)) {
            return Collections.emptyList();
        }

        // Expression statement
        Expr expr = ((ExprStmt) statement).getExpr();
        if (!(expr instanceof CallExpr)) {
            return Collections.emptyList();
        }

        return Arrays.asList((CxxCall) CxxJoinpoints.create((CallExpr) expr, this));
    }

    @Override
    public List<? extends ACall> selectCall() {
        return stmt.getDescendantsAndSelfStream()
                .filter(node -> node instanceof CallExpr)
                .map(loop -> (CxxCall) CxxJoinpoints.create(loop, this))
                .collect(Collectors.toList());
    }

    @Override
    public Boolean getIsFirstImpl() {
        // Get parent and check Stmt position on that list
        return stmt.getParent().getChildren(Stmt.class).indexOf(stmt) == 0;

        // return stmt.indexOfSelf() == 1;
        // List<? extends AStatement> statementJps = parent.selectStatements();
        // Preconditions.checkArgument(!statementJps.isEmpty(), "Expected parent to ");
    }

    @Override
    public Boolean getIsLastImpl() {
        // Get parent and check Stmt position on that list
        List<Stmt> siblings = stmt.getParent().getChildren(Stmt.class);
        return siblings.indexOf(stmt) == (siblings.size() - 1);

        // return stmt.indexOfSelf() == stmt.getParentImpl().numChildren();
    }

    @Override
    public List<? extends AArrayAccess> selectArrayAccess() {
        return stmt.getDescendantsStream()
                .filter(ArraySubscriptExpr.class::isInstance)
                .map(ArraySubscriptExpr.class::cast)
                .filter(CxxStatement::isTopLevelArraySubscript)
                .map(arraySub -> (CxxArrayAccess) CxxJoinpoints.create(arraySub, this))
                .collect(Collectors.toList());

    }

    private static boolean isTopLevelArraySubscript(ArraySubscriptExpr expr) {
        ClavaNode parent = ClavaNodes.getParentNormalized(expr);
        if (!(parent instanceof ArraySubscriptExpr)) {
            return true;
        }

        // Parent can be an ArraySubscriptExpr, if expr is not the first child
        ClavaNode normalizedFirstChild = ClavaNodes.normalize(parent.getChild(0));

        return normalizedFirstChild != expr;
    }

    @Override
    public List<? extends AVardecl> selectVardecl() {
        return CxxSelects.select(AVardecl.class, stmt.getChildren(), true, this, VarDecl.class);

    }

    @Override
    public List<? extends AVarref> selectVarref() {
        return CxxSelects.select(AVarref.class, stmt.getChildren(), true, this,
                // node -> node instanceof DeclRefExpr && !((DeclRefExpr) node).isFunctionCall());
                CxxStatement::isVarref);
    }

    private static boolean isVarref(ClavaNode node) {
        // Must be a DeclRefExpr
        if (!(node instanceof DeclRefExpr)) {
            return false;
        }

        DeclRefExpr declRefExpr = (DeclRefExpr) node;

        // Filter DeclRefExprs that are a FunctionCall
        if (declRefExpr.isFunctionCall()) {
            return false;
        }

        // Filter DeclRefExprs that do not have DeclaratorDecl as decls
        Optional<? extends Decl> declTry = declRefExpr.getDeclaration();
        if (!declTry.isPresent()) {
            SpecsLogs.msgInfo("Could not find declaration for reference " + declRefExpr.getRefName() + " at "
                    + declRefExpr.getLocation());
            return false;
        }

        // declRefExpr.getDeclaration()
        // .orElseThrow(() -> new RuntimeException("Could not find declaration of " + declRefExpr.getExprData));
        // if (!(declRefExpr.getDeclaration() instanceof DeclaratorDecl)) {
        if (!(declTry.get() instanceof DeclaratorDecl)) {
            return false;
        }

        return true;
    }

    @Override
    public List<? extends AExpression> selectExpr() {
        return CxxSelects.select(AExpression.class, stmt.getChildren(), true, this, Expr.class);
    }

    @Override
    public List<? extends AExpression> selectChildExpr() {
        // Since it is direct, normalize children

        return CxxSelects.select(AExpression.class, stmt.getChildrenNormalized(), false, this, Expr.class);
    }

    @Override
    public List<? extends ABinaryOp> selectBinaryOp() {
        return CxxSelects.select(ABinaryOp.class, stmt.getChildrenNormalized(), true, this, BinaryOperator.class);
    }

    @Override
    public List<? extends AUnaryOp> selectUnaryOp() {
        return CxxSelects.select(AUnaryOp.class, stmt.getChildrenNormalized(), true, this, UnaryOperator.class);
    }

}
