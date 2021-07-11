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
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CXXMemberCallExpr;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.MemberExpr;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACall;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunction;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunctionType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMemberAccess;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.actions.CallWrap;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class CxxCall extends ACall {

    private final CallExpr call;

    public CxxCall(CallExpr call) {
        super(new CxxExpression(call));

        this.call = call;
    }

    @Override
    public String getNameImpl() {
        return call.getCalleeNameTry().orElse(null);
    }

    @Override
    public Integer getNumArgsImpl() {
        return call.getArgs().size();
    }

    @Override
    public List<? extends AExpression> selectArg() {
        return call.getArgs().stream()
                .map(expr -> CxxJoinpoints.create(expr, AExpression.class))
                .collect(Collectors.toList());
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

            // VarDeclData varDeclData = new VarDeclData(StorageClass.NONE, TLSKind.NONE, false, false,
            // InitializationStyle.CINIT, false);
            // DeclData declData = new DeclData(false, false, true, false, false, false);
            // VarDecl varDecl = ClavaNodeFactory.varDecl(varDeclData, variableName, returnType, declData,
            // call.getInfo(),
            // call);

            VarDecl varDecl = getFactory().varDecl(variableName, returnType);
            varDecl.setInit(call);
            varDecl.set(VarDecl.IS_USED);

            DeclStmt declStmt = call.getFactoryWithNode().declStmt(varDecl);
            // DeclStmt declStmt = ClavaNodeFactory.declStmt(call.getInfo(), Arrays.asList(varDecl));

            // Replace stmt
            NodeInsertUtils.replace(exprStmt, declStmt, true);
        }
        // If assignment to already existing variable, use the following tree:
        // ExprStmt -> BinaryOperator -> DeclRefExpr, Call
        else {
            Expr varExpr = CxxWeaver.getFactory().literalExpr(variableName, returnType);
            BinaryOperator assign = CxxWeaver.getFactory().binaryOperator(BinaryOperatorKind.Assign, returnType,
                    varExpr, call);
            // BinaryOperator assign = ClavaNodeFactory.binaryOperator(BinaryOperatorKind.ASSIGN, new
            // ExprData(returnType),
            // call.getInfo(), varExpr, call);
            ExprStmt newStmt = CxxWeaver.getFactory().exprStmt(assign);

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
    public AType getTypeImpl() {
        if (call instanceof CXXMemberCallExpr) {
            return CxxJoinpoints.create(((CXXMemberCallExpr) call).getType(), AType.class);
        }

        // Return the type of the function (return type)
        Type calleeType = call.getCallee().getType();
        // System.out.println("CALLEE:" + call.getCallee());
        // If PointerType to FunctionType, remove pointer
        // if (calleeType instanceof PointerType && ((PointerType) calleeType).getPointeeType() instanceof FunctionType)
        // {
        // calleeType = ((PointerType) calleeType).getPointeeType();
        // }
        // System.out.println("CALLEE TYPE:" + calleeType);
        if (calleeType instanceof FunctionType) {
            return CxxJoinpoints.create(((FunctionType) calleeType).getReturnType(), AType.class);
        }

        /*
        if (!(calleeType instanceof LiteralType)) {
            LoggingUtils
                    .msgWarn("Expected LiteralType, got '" + calleeType.getClass().getSimpleName() + "'. Check if ok");
        }
        */

        return CxxJoinpoints.create(calleeType, AType.class);
    }

    @Override
    public List<? extends AExpression> selectCallee() {
        return Arrays.asList(CxxJoinpoints.create(call.getCallee(), AExpression.class));
    }

    @Override
    public String[] getMemberNamesArrayImpl() {
        return call.getCallMemberNames().toArray(new String[0]);
    }

    @Override
    public void setNameImpl(String name) {
        defNameImpl(name);
    }

    @Override
    public void defNameImpl(String value) {
        call.setCallName(value);
    }

    @Override
    public AFunction getDeclarationImpl() {
        return call.getDeclaration().map(decl -> (AFunction) CxxJoinpoints.create(decl)).orElse(null);
        // Optional<DeclaratorDecl> varDecl = call.getCalleeDeclRef().getVariableDeclaration();
        //
        // return varDecl.map(decl -> CxxJoinpoints.create(decl, this)).orElse(null);
    }

    @Override
    public AFunction getDefinitionImpl() {
        return call.getDefinition().map(decl -> (AFunction) CxxJoinpoints.create(decl)).orElse(null);
    }

    @Override
    public AExpression[] getArgsArrayImpl() {
        return call.getArgs()
                .stream()
                // .map(Expr::getCode)
                .map(arg -> (AExpression) CxxJoinpoints.create(arg))
                .collect(Collectors.toList())
                .toArray(new AExpression[0]);
    }

    @Override
    public AExpression[] getArgListArrayImpl() {
        return getArgsArrayImpl();
    }

    @Override
    public AType getReturnTypeImpl() {

        return (AType) CxxJoinpoints.create(call.getType());
    }

    @Override
    public void wrapImpl(String name) {
        new CallWrap(this).addWrapper(name);
    }

    @Override
    public void inlineImpl() {
        // Only inline if call is associated to an App
        if (!call.getAppTry().isPresent()) {
            SpecsLogs.msgInfo("Tried to inline call that is not associated to an app");
            return;
        }

        call.getApp().inline(call);
        // call.getAncestor(App.class).inline(call);
        // new CallInliner(call).inline();
    }

    @Override
    public void setArgFromStringImpl(int index, String expr) {
        // Get arg of equivalent index, to extract type
        Expr arg = call.getArgs().get(index);
        Expr literalExpr = CxxWeaver.getFactory().literalExpr(expr, arg.getExprType());
        setArgImpl(index, (AExpression) CxxJoinpoints.create(literalExpr));
    }

    @Override
    public void addArgImpl(String arg, AJoinPoint type) {

        // Check if joinpoint is a CxxType
        if (!(type instanceof AType)) {
            SpecsLogs.msgInfo("addArgImpl: the provided join point (" + type.getJoinPointType() + ") is not a type");
            return;
        }

        Type typeNode = (Type) type.getNode();

        Expr literalExpr = CxxWeaver.getFactory().literalExpr(arg, typeNode);

        call.addChild(literalExpr);
    }

    @Override
    public void setArgImpl(Integer index, AExpression expr) {
        // Check num args
        // int numArgs = getArgListArrayImpl().length;
        // if (index >= 0 && index < numArgs) {
        // SpecsLogs.msgInfo(
        // "Not setting call argument, index is '" + index + "' and call has " + numArgs + " arguments");
        // return;
        // }

        call.setArgument(index, (Expr) expr.getNode());
    }

    @Override
    public AExpression argImpl(int index) {
        call.checkIndex(index);
        Expr arg = call.getArgs().get(index);
        return (AExpression) CxxJoinpoints.create(arg);

    }

    @Override
    public Boolean getIsMemberAccessImpl() {
        return call instanceof CXXMemberCallExpr;
    }

    @Override
    public AMemberAccess getMemberAccessImpl() {
        if (!(call instanceof CXXMemberCallExpr)) {
            return null;
        }

        MemberExpr memberExpr = ((CXXMemberCallExpr) call).getCallee();

        return CxxJoinpoints.create(memberExpr, AMemberAccess.class);

    }

    @Override
    public AFunctionType getFunctionTypeImpl() {
        return call.getFunctionType()
                .map(type -> (AFunctionType) CxxJoinpoints.create(type))
                .orElse(null);

        // return (AType) CxxJoinpoints.create(call.getFunctionType(), this);
    }

    @Override
    public Boolean getIsStmtCallImpl() {
        return call.isStmtCall();
    }

    @Override
    public AFunction getFunctionImpl() {
        // return CxxJoinpoints.create(call.getFunctionDecl(), this, AFunction.class);
        return call.getFunctionDecl()
                .map(fDecl -> CxxJoinpoints.create(fDecl, AFunction.class))
                .orElse(null);
        // return CxxJoinpoints.create(call.getFunction(), this, AFunction.class);
    }

    @Override
    public String getSignatureImpl() {
        AFunction function = getFunctionImpl();

        if (function != null) {
            return function.getSignatureImpl();
        }

        // if (getDeclarationImpl() != null) {
        // System.out.println("DECL SIG:" + getDeclarationImpl().getSignatureImpl());
        // }
        // System.out.println("DECL:" + getDeclarationImpl());
        // System.out.println("DEF:" + getDefinitionImpl());

        return "<" + getNameImpl() + ">";
    }

    @Override
    public AFunction getDeclImpl() {
        return call.getFunctionDecl()
                .map(fDecl -> CxxJoinpoints.create(fDecl, AFunction.class))
                .orElse(null);
    }

    // @Override
    // public String getSignatureImpl() {
    // return call.getSignature();
    // }
}
