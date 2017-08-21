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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.DeclaratorDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.InitializationStyle;
import pt.up.fe.specs.clava.ast.decl.data.StorageClass;
import pt.up.fe.specs.clava.ast.decl.data.VarDeclData;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.CXXMemberCallExpr;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.TLSKind;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACall;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.actions.CallWrap;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class CxxCall extends ACall {

    private final CallExpr call;
    private final ACxxWeaverJoinPoint parent;

    public CxxCall(CallExpr call, ACxxWeaverJoinPoint parent) {
        super(new CxxExpression(call, parent));

        this.call = call;
        this.parent = parent;
    }

    @Override
    public String getNameImpl() {
        return call.getCalleeName();
    }

    @Override
    public Integer getNumArgsImpl() {
        return call.getArgs().size();
    }

    @Override
    public List<? extends AExpression> selectArg() {
        return call.getArgs().stream()
                .map(expr -> CxxJoinpoints.create(expr, this, AExpression.class))
                .collect(Collectors.toList());
    }

    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        return parent;
    }

    @Override
    public CallExpr getNode() {
        return call;
    }

    public void extractImpl(String variableName, Boolean declareVariable) {
        // Check that call is inside an ExprStmt
        if (!(call.getParent() instanceof ExprStmt)) {
            SpecsLogs.msgInfo("Action currently supported only for calls alone in a statement. Skipping for code:"
                    + call.getAncestor(Stmt.class).getCode());
            return;
        }

        // Create assignment to variable
        Type returnType = call.getType();
        ClavaNode exprStmt = call.getParent();
        call.detach();

        // If variable needs to be declared, use following tree:
        // DeclStmt -> VarDecl -> Call
        if (declareVariable) {

            VarDeclData varDeclData = new VarDeclData(StorageClass.NONE, TLSKind.NONE, false, false,
                    InitializationStyle.CINIT);
            DeclData declData = new DeclData(false, false, true, false, false, false);
            VarDecl varDecl = ClavaNodeFactory.varDecl(varDeclData, variableName, returnType, declData, call.getInfo(),
                    call);

            DeclStmt declStmt = ClavaNodeFactory.declStmt(call.getInfo(), Arrays.asList(varDecl));

            // Replace stmt
            NodeInsertUtils.replace(exprStmt, declStmt, true);
        }
        // If assignment to already existing variable, use the following tree:
        // ExprStmt -> BinaryOperator -> DeclRefExpr, Call
        else {
            Expr varExpr = ClavaNodeFactory.literalExpr(variableName, returnType);
            BinaryOperator assign = ClavaNodeFactory.binaryOperator(BinaryOperatorKind.ASSIGN, new ExprData(returnType),
                    call.getInfo(), varExpr, call);
            ExprStmt newStmt = ClavaNodeFactory.exprStmt(assign);

            // Replace stmt
            NodeInsertUtils.replace(exprStmt, newStmt, true);
            /*
            ExprStmt: (0x46d4420)
             BinaryOperator: (0x46d4420) types:int, valueKind:L_VALUE, op:ASSIGNMENT
              DeclRefExpr: (0x46d43d8) types:int, valueKind:L_VALUE, refType:Var, refName:samples, type2:<same as type>
              IntegerLiteral: (0x46d4400) types:int, valueKind:R_VALUE
             */
        }
    }

    @Override
    public AJoinPoint getTypeImpl() {
        if (call instanceof CXXMemberCallExpr) {
            return CxxJoinpoints.create(((CXXMemberCallExpr) call).getType(), this);
        }

        // Return the type of the function (return type)
        Type calleeType = call.getCallee().getType();

        if (calleeType instanceof FunctionType) {
            return CxxJoinpoints.create(((FunctionType) calleeType).getReturnType(), this);
        }

        /*
        if (!(calleeType instanceof LiteralType)) {
            LoggingUtils
                    .msgWarn("Expected LiteralType, got '" + calleeType.getClass().getSimpleName() + "'. Check if ok");
        }
        */

        return CxxJoinpoints.create(calleeType, this);
    }

    @Override
    public List<? extends AExpression> selectCallee() {
        return Arrays.asList(CxxJoinpoints.create(call.getCallee(), this, AExpression.class));
    }

    @Override
    public String[] getMemberNamesArrayImpl() {
        return call.getCallMemberNames().toArray(new String[0]);
    }

    @Override
    public void setNameImpl(String name) {
        call.setCalleeName(name);
    }

    @Override
    public AJoinPoint getDeclImpl() {
        Optional<DeclaratorDecl> varDecl = call.getCalleeDeclRef().getVariableDeclaration();

        return varDecl.map(decl -> CxxJoinpoints.create(decl, this)).orElse(null);
    }

    @Override
    public String[] getArgListArrayImpl() {
        return call.getArgs()
                .stream()
                .map(Expr::getCode)
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    @Override
    public AJoinPoint getReturnTypeImpl() {

        return CxxJoinpoints.create(call.getType(), this);
    }

    @Override
    public void wrapImpl(String name) {
        new CallWrap(this).addWrapper(name);
    }
}
