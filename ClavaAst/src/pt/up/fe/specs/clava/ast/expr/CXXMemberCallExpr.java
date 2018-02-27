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

package pt.up.fe.specs.clava.ast.expr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.RecordType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.exceptions.UnexpectedChildExpection;
import pt.up.fe.specs.util.exceptions.CaseNotDefinedException;

/**
 * Represents a call to a member function that may be written either with member call syntax (e.g., "obj.func()" or
 * "objptr->func()") or with normal function-call syntax ("func()") within a member function that ends up calling a
 * member function.
 * 
 * The callee in either case is a MemberExpr that contains both the object argument and the member function, while the
 * arguments are the arguments within the parentheses (not including the object argument).
 * 
 * @author JoaoBispo
 *
 */
public class CXXMemberCallExpr extends CallExpr {

    public CXXMemberCallExpr(ExprData exprData, ClavaNodeInfo info, MemberExpr function, List<? extends Expr> args) {
        super(exprData, info, function, args);
    }

    /**
     * Private constructor for copy.
     * 
     * @param valueKind
     * @param type
     * @param info
     */
    private CXXMemberCallExpr(ExprData exprData, ClavaNodeInfo info) {
        super(exprData, info, Collections.emptyList());
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXMemberCallExpr(getExprData(), getInfo());
    }

    @Override
    public MemberExpr getCallee() {
        return (MemberExpr) super.getCallee();
    }

    @Override
    public String getCalleeName() {
        ClavaNode callee = getCallee();

        if (!(callee instanceof MemberExpr)) {
            throw new UnexpectedChildExpection(CXXMemberCallExpr.class, callee);
        }

        return ((MemberExpr) callee).getMemberName();
    }

    @Override
    public void setCallName(String name) {
        ClavaNode callee = getCallee();

        if (!(callee instanceof MemberExpr)) {
            throw new UnexpectedChildExpection(CXXMemberCallExpr.class, callee);
        }

        ((MemberExpr) callee).setMemberName(name);
    }

    @Override
    public List<String> getCallMemberNames() {
        List<String> memberNames = new ArrayList<>();

        getCallMemberNames(getCallee(), memberNames);

        return memberNames;
    }

    private void getCallMemberNames(Expr currentNode, List<String> memberNames) {
        // If node is a DeclRefExpr, add its refName to the head of the list and return
        if (currentNode instanceof DeclRefExpr) {
            memberNames.add(((DeclRefExpr) currentNode).getRefName());
            return;
        }

        // If node is a MemberExpr, add its member name to the list after adding its base
        if (currentNode instanceof MemberExpr) {
            getCallMemberNames(((MemberExpr) currentNode).getBase(), memberNames);
            memberNames.add(((MemberExpr) currentNode).getMemberName());
            return;
        }

        throw new CaseNotDefinedException(currentNode.getClass());
    }

    public Expr getBase() {
        return getCallee().getBase();
    }

    public Expr getRootBase() {
        Expr currentExpr = this;
        while (currentExpr instanceof CXXMemberCallExpr) {
            currentExpr = ((CXXMemberCallExpr) currentExpr).getBase();
        }

        return currentExpr;
    }

    @Override
    protected Optional<FunctionDecl> getFunctionDecl() {
        // Get root base
        Expr rootBase = getRootBase();
        if (rootBase != getBase()) {
            ClavaLog.warning("Not yet implemented for consecutive chains");
            return Optional.empty();
        }

        DeclRefExpr rootDeclRef = rootBase.getFirstDescendantsAndSelf(DeclRefExpr.class).orElse(null);

        if (rootDeclRef == null) {
            ClavaLog.warning("Expected a DeclRefExpr, got:\n" + rootBase);
            return Optional.empty();
        }

        // Get recordType of declaration

        // rootDeclRef.getVariableDeclaration().

        // Type initExprType = initExpr.getType();
        // RecordType recordType = initExprType instanceof RecordType ? (RecordType) initExprType
        // : initExprType.desugarTo(RecordType.class);
        //

        if (!(rootDeclRef.getType() instanceof RecordType)) {
            ClavaLog.warning("Expected a RecordType, got:\n" + rootDeclRef.getType());
            return Optional.empty();
        }

        RecordType recordType = (RecordType) rootDeclRef.getType();

        // RecordType recordType = initExpr.getType().desugarTo(RecordType.class);
        CXXRecordDecl recordDecl = getApp().getCXXRecordDeclTry(recordType).orElse(null);

        if (recordDecl == null) {
            return Optional.empty();
        }
        // getApp().getFunctionDeclaration(declName, functionType)
        // System.out.println("RECORD DECL:" + recordDecl);

        // Get methods with same name
        List<CXXMethodDecl> methods = recordDecl.getMethod(getCalleeName());

        List<Type> argTypes = getArgs().stream()
                .map(Expr::getType)
                .collect(Collectors.toList());

        // Choose the method with same argument types
        for (CXXMethodDecl methodDecl : methods) {
            FunctionProtoType functionType = methodDecl.getFunctionType();

            List<Type> paramTypes = functionType.getParamTypes();

            // Check number of arguments
            if (paramTypes.size() != argTypes.size()) {
                continue;
            }

            // Compare each type
            boolean paramsAreEqual = true;
            for (int i = 0; i < paramTypes.size(); i++) {
                // System.out.println("COMPARING\n" + paramTypes.get(i) + "\nWITH\n" + argTypes.get(i));
                boolean areEqual = paramTypes.get(i).equals(argTypes.get(i));
                // System.out.println("ARE EQUAL? " + areEqual);
                if (!areEqual) {
                    paramsAreEqual = false;
                    break;
                }
            }

            if (paramsAreEqual) {
                return Optional.of(methodDecl);
            }
        }

        // System.out.println("ROOT:" + rootDeclRef);
        // System.out.println("TYPE:" + rootDeclRef.getType());
        // System.out.println("MEMBER CALL:" + this);
        // System.out.println("DECLARATION:" + rootDeclRef.getDeclaration());
        // System.out.println(getArgs());
        // Follow the chain

        // System.out.println("MEMBER CALL TYPE:" + getCallee().getType());
        // DeclRefExpr declRefExpr = getCalleeDeclRef();
        // System.out.println("Decl ref expr:" + declRefExpr);

        return Optional.empty();
        /*
        Optional<DeclaratorDecl> varDecl = getCalleeDeclRef().getVariableDeclaration();
        
        System.out.println("VARDECL:" + varDecl);
        
        if (!varDecl.isPresent()) {
            return Optional.empty();
        }
        
        DeclaratorDecl declarator = varDecl.get();
        if (declarator instanceof FunctionDecl) {
            return Optional.of((FunctionDecl) declarator);
        }
        
        SpecsLogs.msgLib("Could not extract function from member call callee decl, check if ok:\n" + declarator);
        return Optional.empty();
        
        // if (!(declarator instanceof FunctionDecl)) {
        // SpecsLogs.msgWarn("Call callee decl is not a function decl, check if ok:\n" + declarator);
        // return Optional.empty();
        // }
        //
        // return Optional.of((FunctionDecl) declarator);
        */
    }
}
