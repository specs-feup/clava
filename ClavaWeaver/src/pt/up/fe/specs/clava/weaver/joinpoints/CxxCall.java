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
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMemberAccess;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.actions.CallWrap;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class CxxCall<Self extends CxxCall<Self>> extends ACall<Self> {

    public CxxCall(CallExpr call, CxxWeaver weaver) {
        super(call, weaver);
    }

    @Override
    public CallExpr getNodeImpl() {
        return (CallExpr) super.getNodeImpl();
    }

    @Override
    public String getNameImpl() {
        return this.getNodeImpl().getCalleeNameTry().orElse(null);
    }

    @Override
    public int getNumArgsImpl() {
        return this.getNodeImpl().getArgs().size();
    }

    public void extractImpl(String variableName, boolean declareVariable) {
        var call = this.getNodeImpl();

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

            VarDecl varDecl = getFactory().varDecl(variableName, returnType);
            varDecl.setInit(call);
            varDecl.set(VarDecl.IS_USED);

            DeclStmt declStmt = call.getFactoryWithNode().declStmt(varDecl);

            // Replace stmt
            NodeInsertUtils.replace(exprStmt, declStmt, true);
        }
        // If assignment to already existing variable, use the following tree:
        // ExprStmt -> BinaryOperator -> DeclRefExpr, Call
        else {
            Expr varExpr = getWeaverEngine().getFactory().literalExpr(variableName, returnType);
            BinaryOperator assign = getWeaverEngine().getFactory().binaryOperator(BinaryOperatorKind.Assign, returnType,
                    varExpr, call);
            ExprStmt newStmt = getWeaverEngine().getFactory().exprStmt(assign);

            // Replace stmt
            NodeInsertUtils.replace(exprStmt, newStmt, true);
            /*
             * ExprStmt: (0x46d4420)
             * BinaryOperator: (0x46d4420) types:int, valueKind:L_VALUE, op:ASSIGNMENT
             * DeclRefExpr: (0x46d43d8) types:int, valueKind:L_VALUE, refType:Var,
             * refName:samples, type2:<same as type>
             * IntegerLiteral: (0x46d4400) types:int, valueKind:R_VALUE
             */
        }
    }

    @Override
    public AType<?> getTypeImpl() {
        var call = this.getNodeImpl();
        if (call instanceof CXXMemberCallExpr) {
            return CxxJoinpoints.create(((CXXMemberCallExpr) call).getType(), getWeaverEngine(), AType.class);
        }

        // Return the type of the function (return type), after desugaring
        Type calleeType = call.getCallee().getType().desugarAll();
        // If PointerType to FunctionType, remove pointer
        if (calleeType instanceof FunctionType) {
            return CxxJoinpoints.create(((FunctionType) calleeType).getReturnType(), getWeaverEngine(), AType.class);
        }

        return CxxJoinpoints.create(calleeType, getWeaverEngine(), AType.class);
    }

    @Override
    public String[] getMemberNamesImpl() {
        return this.getNodeImpl().getCallMemberNames().toArray(new String[0]);
    }

    @Override
    public void setNameImpl(String name) {
        this.getNodeImpl().setCallName(name);
    }

    @Override
    public AFunction<?> getDeclarationImpl() {
        return this.getNodeImpl().getPrototypes().stream()
                .map(decl -> CxxJoinpoints.create(decl,
                        getWeaverEngine(), AFunction.class))
                .findFirst()
                .orElse(null);
    }

    @Override
    public AFunction<?> getDefinitionImpl() {
        return this.getNodeImpl().getDefinition().map(decl -> CxxJoinpoints.create(decl, getWeaverEngine(), AFunction.class))
                .orElse(null);
    }

    @Override
    public AExpression<?>[] getArgsImpl() {
        return this.getNodeImpl().getArgs()
                .stream()
                // .map(Expr::getCode)
                .map(arg -> CxxJoinpoints.create(arg, getWeaverEngine(), AExpression.class))
                .collect(Collectors.toList())
                .toArray(new AExpression[0]);
    }

    @Override
    public AExpression<?>[] getArgListImpl() {
        return getArgsImpl();
    }

    @Override
    public AType<?> getReturnTypeImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getType(), getWeaverEngine(), AType.class);
    }

    @Override
    public void wrapImpl(String name) {
        new CallWrap(getWeaverEngine(), this).addWrapper(name);
    }

    @Override
    public boolean inlineImpl() {
        var call = this.getNodeImpl();

        // Only inline if call is associated to an App
        if (!call.getAppTry().isPresent()) {
            SpecsLogs.msgInfo("Tried to inline call that is not associated to an app");
            return false;
        }

        return call.getApp().inline(call);
    }

    @Override
    public void setArgFromStringImpl(int index, String expr) {
        // Get arg of equivalent index, to extract type
        Expr arg = this.getNodeImpl().getArgs().get(index);
        Expr literalExpr = getWeaverEngine().getFactory().literalExpr(expr, arg.getExprType());
        setArgImpl(index, CxxJoinpoints.create(literalExpr, getWeaverEngine(), AExpression.class));
    }

    @Override
    public void addArgImpl(String arg, AType<?> type) {
        Type processedType;

        if (type == null) {
            processedType = getWeaverEngine().getFactory().dummyType("from $call.addArg()");
        } else {
            processedType = (Type) type.getNodeImpl();
        }

        this.getNodeImpl().addArgument(arg, processedType);
    }

    @Override
    public void addArgImpl(String arg, String type) {
        this.getNodeImpl().addArgument(arg, getWeaverEngine().getFactory().literalType(type));
    }

    @Override
    public void setArgImpl(int index, AExpression<?> expr) {
        this.getNodeImpl().setArgument(index, (Expr) expr.getNodeImpl());
    }

    @Override
    public AExpression<?> getGetArgImpl(int index) {
        this.getNodeImpl().checkIndex(index);
        Expr arg = this.getNodeImpl().getArgs().get(index);
        return CxxJoinpoints.create(arg, getWeaverEngine(), AExpression.class);
    }

    @Override
    public boolean getIsMemberAccessImpl() {
        return this.getNodeImpl() instanceof CXXMemberCallExpr;
    }

    @Override
    public AMemberAccess<?> getMemberAccessImpl() {
        if (!(this.getNodeImpl() instanceof CXXMemberCallExpr)) {
            return null;
        }

        var callee = ((CXXMemberCallExpr) this.getNodeImpl()).getCallee();

        MemberExpr memberExpr = callee;

        return CxxJoinpoints.create(memberExpr, getWeaverEngine(), AMemberAccess.class);
    }

    @Override
    public AFunctionType<?> getFunctionTypeImpl() {
        return this.getNodeImpl().getFunctionType()
                .map(type -> CxxJoinpoints.create(type, getWeaverEngine(), AFunctionType.class))
                .orElse(null);
    }

    @Override
    public boolean getIsStmtCallImpl() {
        return this.getNodeImpl().isStmtCall();
    }

    @Override
    public AFunction<?> getFunctionImpl() {
        // First, try the implementation
        var definition = getDefinitionImpl();

        if (definition != null) {
            return definition;
        }

        // Implementation not found return declaration
        return getDeclarationImpl();
    }

    @Override
    public String getSignatureImpl() {
        AFunction<?> function = getFunctionImpl();

        if (function != null) {
            return function.getSignatureImpl();
        }

        return "<" + getNameImpl() + ">";
    }

    @Override
    public AFunction<?> getDeclImpl() {
        return this.getNodeImpl().getFunctionDecl()
                .map(fDecl -> CxxJoinpoints.create(fDecl,
                        getWeaverEngine(), AFunction.class))
                .orElse(null);
    }

    @Override
    public AFunction<?> getDirectCalleeImpl() {
        return this.getNodeImpl().get(CallExpr.DIRECT_CALLEE)
                .map(callee -> CxxJoinpoints.create(callee,
                        getWeaverEngine(), AFunction.class))
                .orElse(null);
    }
}
