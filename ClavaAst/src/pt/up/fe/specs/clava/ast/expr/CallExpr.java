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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.exceptions.UnexpectedChildExpection;
import pt.up.fe.specs.util.SpecsCollections;

/**
 * Represents a function call.
 *
 * @author JoaoBispo
 *
 */
public class CallExpr extends Expr {

    public CallExpr(ExprData exprData, ClavaNodeInfo info, Expr function, List<? extends Expr> args) {
        this(exprData, info, SpecsCollections.concat(function, SpecsCollections.cast(args, ClavaNode.class)));
    }

    protected CallExpr(ExprData exprData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {

        super(exprData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CallExpr(getExprData(), getInfo(), Collections.emptyList());
    }

    @Override
    public String getCode() {

        // System.out.println("CALLEXPR");
        // System.out.println("CALLEE CODE:" + getCalleeCode());
        // System.out.println("ARGS CODE:" + getArgsCode());

        return getCalleeCode() + getArgsCode();
    }

    protected String getArgsCode() {
        String argsCode = getArgs().stream()
                // Remove default arguments
                .filter(arg -> !(arg instanceof CXXDefaultArgExpr))
                .map(arg -> arg.getCode())
                // .map(arg -> arg instanceof CXXDefaultArgExpr ? "/* DEFAULT ARG */" : arg.getCode())
                .collect(Collectors.joining(", ", "(", ")"));
        return argsCode;
    }

    protected String getCalleeCode() {
        return getCallee().getCode();
    }

    public Expr getCallee() {
        return getChild(Expr.class, 0);
    }

    private DeclRefExpr getCalleeInternal() {
        Expr callee = getCallee();

        if (!(callee instanceof DeclRefExpr)) {
            throw new UnexpectedChildExpection(CallExpr.class, callee);
        }

        return (DeclRefExpr) callee;
    }

    public List<Expr> getArgs() {
        if (getNumChildren() == 1) {
            return Collections.emptyList();
        }

        return SpecsCollections.cast(getChildren().subList(1, getNumChildren()), Expr.class);
    }

    public DeclRefExpr getCalleeDeclRef() {
        return getCallee().getFirstDescendantsAndSelf(DeclRefExpr.class).orElseThrow(
                () -> new RuntimeException(
                        "Expected callee tree to have at least one DeclRefExpr:\n" + getCallee()));
    }

    public String getCalleeName() {
        return getCalleeDeclRef().getRefName();
    }

    /**
     * For simple CallExprs, this is equivalent to the callee name.
     *
     * @return
     */
    public List<String> getCallMemberNames() {
        return Arrays.asList(getCalleeName());
    }

    public void setCalleeName(String name) {
        getCalleeDeclRef().setRefName(name);
    }
}
