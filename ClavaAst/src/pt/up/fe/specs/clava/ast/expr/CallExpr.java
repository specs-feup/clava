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
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.ValueDecl;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.utils.Nameable;
import pt.up.fe.specs.util.SpecsCollections;

/**
 * Represents a function call.
 *
 * @author JoaoBispo
 *
 */
public class CallExpr extends Expr {

    /// DATAKEYS BEGIN

    public final static DataKey<Optional<FunctionDecl>> DIRECT_CALLEE = KeyFactory.optional("directCallee");

    /// DATAKEYS END

    public CallExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
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

    public void setCallee(Expr callee) {
        if (callee instanceof DeclRefExpr) {
            ValueDecl decl = callee.get(DeclRefExpr.DECL);
            if (decl instanceof FunctionDecl) {
                set(DIRECT_CALLEE, Optional.of((FunctionDecl) decl));
            } else {
                set(DIRECT_CALLEE, Optional.empty());
            }
        } else {
            set(DIRECT_CALLEE, Optional.empty());
        }

        setChild(0, callee);
    }

    public List<Expr> getArgs() {
        if (getNumChildren() == 1) {
            return Collections.emptyList();
        }

        return SpecsCollections.cast(getChildren().subList(1, getNumChildren()), Expr.class);
    }

    public int getNumArgs() {
        return getNumChildren() - 1;
    }

    public void checkIndex(int index) {
        int numArgs = getNumArgs();
        boolean validIndex = index >= 0 && index < numArgs;
        if (!validIndex) {
            throw new RuntimeException(
                    "Not setting call argument, index is '" + index + "' and call has " + numArgs + " arguments");

        }
    }

    public Expr setArgument(int index, Expr arg) {
        // Check num args
        checkIndex(index);
        int argIndex = 1 + index;
        return (Expr) setChild(argIndex, arg);
    }

    public void setArguments(List<Expr> args) {
        // Remove current args
        if (getNumArgs() > 0) {
            removeChildren(1, getNumChildren());
        }

        // Set new args
        addChildren(args);
    }

    public DeclRefExpr getCalleeDeclRef() {
        return getCalleeDeclRefTry().orElseThrow(
                () -> new RuntimeException(
                        "Expected callee tree to have at least one DeclRefExpr:\n" + getCallee()));
    }

    public Optional<DeclRefExpr> getCalleeDeclRefTry() {
        return getCallee().getFirstDescendantsAndSelf(DeclRefExpr.class);
    }

    /**
     * 
     * @return the prototypes of this function call
     */
    public List<FunctionDecl> getPrototypes() {
        return getFunctionDecl().map(FunctionDecl::getPrototypes).orElse(Collections.emptyList());
    }

    /**
     * The FunctionDecl as given by Clang. Usually it is the first that appears in the code.
     * 
     * @return
     */
    public Optional<FunctionDecl> getFunctionDecl() {
        return get(DIRECT_CALLEE)
                // If FunctionDecl has a primary template decl (e.g., is a template specialization), return original
                // template instead
                .map(ClavaNodes::normalizeDecl)
                .map(FunctionDecl.class::cast);
    }

    /**
     * 
     * @return the definition of this function call, if present
     */
    public Optional<FunctionDecl> getDefinition() {
        return getFunctionDecl().flatMap(FunctionDecl::getImplementation);
    }

    /**
     * 
     * @return can return
     */
    public String getCalleeName() {
        return getCalleeNameTry()
                .orElseThrow(() -> new RuntimeException("Could not find callee name for node:" + getCallee()));
    }

    public Optional<String> getCalleeNameTry() {

        Optional<Nameable> nameable = getCallee().getDescendantsAndSelfStream()
                .filter(Nameable.class::isInstance)
                .findFirst()
                .map(Nameable.class::cast);

        if (nameable.isPresent()) {
            return nameable.map(Nameable::getName);
        }

        return Optional.empty();
    }

    /**
     * For simple CallExprs, this is equivalent to the callee name.
     *
     * @return
     */
    public List<String> getCallMemberNames() {
        return Arrays.asList(getCalleeName());
    }

    /**
     * Sets the name of the call.
     * 
     * @param name
     */
    public void setCallName(String name) {
        getCalleeDeclRef().setRefName(name);
    }

    public Optional<FunctionType> getFunctionType() {

        var decls = getFunctionDecl().map(decl -> decl.getDecls()).orElse(Collections.emptyList());

        return decls.stream().map(FunctionDecl::getFunctionType).findFirst();
    }

    /**
     * 
     * @return true if this call is the only expression of a statement
     */
    public boolean isStmtCall() {
        if (!hasParent()) {
            return false;
        }

        return getParent() instanceof Stmt;
    }

}
