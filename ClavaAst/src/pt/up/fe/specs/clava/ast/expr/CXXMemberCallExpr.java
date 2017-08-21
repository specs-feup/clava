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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
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

}
