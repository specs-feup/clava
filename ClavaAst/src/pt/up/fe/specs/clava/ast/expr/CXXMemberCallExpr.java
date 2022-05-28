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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXConversionDecl;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.exceptions.UnexpectedChildExpection;
import pt.up.fe.specs.util.collections.SpecsList;
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

    /// DATAKEYS BEGIN

    public final static DataKey<Optional<CXXMethodDecl>> METHOD_DECL = KeyFactory.optional("methodDecl");

    /// DATAKEYS END

    // private final static List<Class<? extends ClavaNode>> MEMBER_BYPASS = Arrays.asList(ParenExpr.class);

    public CXXMemberCallExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /**
     * Can either return a MemberExpr, or a pseudo-object PointerToMemberExpr, that is created from a ParenExpr that has
     * inside a pointer-to-member binary operation.
     */
    @Override
    public MemberExpr getCallee() {
        var callee = super.getCallee();

        // If callee is a MemberExpr, just return
        if (callee instanceof MemberExpr) {
            return (MemberExpr) callee;
        }

        // Check if ParenExpr with a binary operator that is a pointer to member

        // System.out.println("CALLEE: " + callee.getLocation());
        // System.out.println("CALLE CODE: " + callee.getCode());
        // System.out.println("CALLEE TYPE: " + callee.getClass());

        if (callee instanceof ParenExpr) {
            var subExpr = ((ParenExpr) callee).getSubExpr();
            // System.out.println("SubExpr: " + subExpr);
            if (subExpr instanceof BinaryOperator) {
                var binOp = (BinaryOperator) subExpr;

                if (binOp.getOp() == BinaryOperatorKind.PtrMemD || binOp.getOp() == BinaryOperatorKind.PtrMemI) {
                    return PointerToMemberExpr.newInstance(binOp);
                }

                // var pointerToMember = PointerToMemberExpr.newInstance(binOp);
                // System.out.println("PTM CODE: " + pointerToMember.getCode());

                // System.out.println("LHS: " + binOp.getLhs().getChild(0));
                // System.out.println("RHS: " + binOp.getRhs().getChild(0));
            }
        }

        throw new RuntimeException("Case not defined to callee of this kind: " + callee);
        // MemberExpr might be encapsulated by a ParenExpr
        // return (MemberExpr) callee.normalize(MEMBER_BYPASS);
    }

    @Override
    // public String getCalleeName() {
    public Optional<String> getCalleeNameTry() {
        ClavaNode callee = getCallee();

        if (!(callee instanceof MemberExpr)) {
            throw new UnexpectedChildExpection(CXXMemberCallExpr.class, callee);
        }

        return Optional.of(((MemberExpr) callee).getMemberName());
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

        // If node is a CXXMemberCallExpr, add its member name to the list after adding its base
        if (currentNode instanceof CXXMemberCallExpr) {
            memberNames.addAll(((CXXMemberCallExpr) currentNode).getCallMemberNames());

            return;
        }

        if (currentNode instanceof CallExpr) {
            memberNames.add(((CallExpr) currentNode).getCalleeName());
            return;
        }

        throw new CaseNotDefinedException(currentNode.getClass());
    }

    public Expr getBase() {
        return getCallee().getBase();
    }

    public Expr getRootBase() {
        return getCallee().getExprChain().get(0);

    }

    @Override
    public Optional<FunctionDecl> getFunctionDecl() {
        return get(METHOD_DECL).map(method -> (FunctionDecl) method);
    }

    /*
    private Optional<FunctionDecl> getFunctionDeclFromRecord(RecordType recordType) {
        CXXRecordDecl recordDecl = getApp().getCXXRecordDeclTry(recordType).orElse(null);
    
        if (recordDecl == null) {
            return Optional.empty();
        }
    
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
    
                boolean areEqual = paramTypes.get(i).equals(argTypes.get(i));
    
                if (!areEqual) {
                    paramsAreEqual = false;
                    break;
                }
            }
    
            if (paramsAreEqual) {
                return Optional.of(methodDecl);
            }
        }
    
        return Optional.empty();
    }
    */

    @Override
    public SpecsList<String> getSignatureCustomStrings() {
        return super.getSignatureCustomStrings().andAdd(getCalleeNameTry().orElse("<no_calle_name>"));
    }

    @Override
    public String getCode() {

        var methodDecl = get(METHOD_DECL);

        // Special case: simplifies generated code, only generates base code
        if (methodDecl.isPresent() && methodDecl.get() instanceof CXXConversionDecl) {
            return getBase().getCode();
        }

        return super.getCode();
    }

}
