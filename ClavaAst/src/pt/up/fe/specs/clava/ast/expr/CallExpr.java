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

    // public CallExpr(ExprData exprData, ClavaNodeInfo info, Expr function, List<? extends Expr> args) {
    // this(exprData, info, SpecsCollections.concat(function, SpecsCollections.cast(args, ClavaNode.class)));
    // }
    //
    // protected CallExpr(ExprData exprData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    //
    // super(exprData, info, children);
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new CallExpr(getExprData(), getInfo(), Collections.emptyList());
    // }

    @Override
    public String getCode() {

        // System.out.println("CALLEXPR");
        // System.out.println("THIS: " + this);
        // System.out.println("CALLEE CODE:" + getCalleeCode());
        // System.out.println("CALLEE: " + getCallee());
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
        // System.out.println("CALLEE: " + getCallee());
        // System.out.println("DIRECT_CALL: " + get(DIRECT_CALLEE));
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

    /*
    private DeclRefExpr getCalleeInternal() {
        Expr callee = getCallee();
    
        if (!(callee instanceof DeclRefExpr)) {
            throw new UnexpectedChildExpection(CallExpr.class, callee);
        }
    
        return (DeclRefExpr) callee;
    }
    */

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

    // /**
    // * @deprecated use getPrototypes() or getFunctionDecl() instead
    // * @return the declaration of this function call, if present
    // */
    // @Deprecated
    // public Optional<FunctionDecl> getDeclaration() {
    // return getPrototypes().stream().findFirst();
    // // Optional<DeclaratorDecl> varDecl = getCalleeDeclRef().getVariableDeclaration();
    // //
    // // if (!varDecl.isPresent()) {
    // // return Optional.empty();
    // // }
    // //
    // // DeclaratorDecl declarator = varDecl.get();
    // // if (!(declarator instanceof FunctionDecl)) {
    // // SpecsLogs.msgWarn("Call callee decl is not a function decl, check if ok:\n" + declarator);
    // // return Optional.empty();
    // // }
    // // Optional<FunctionDecl> functionDecl = getFunctionDecl();
    // // System.out.println("CALL EXPR getDeclaration: " + getFunctionDecl());
    // // System.out.println("CALL EXPR getDeclaration.getDeclaration: " + getFunctionDecl().get().getDeclaration());
    //
    // // return getFunctionDecl().map(FunctionDecl::getPrototypes).flatMap(list -> !list.isEmpty() ?
    // // Optional.of(list.get(0) : Optional.empty());
    // // if (!functionDecl.isPresent()) {
    // // return Optional.empty();
    // // }
    // //
    // // // If no body, return immediately
    // // if (!functionDecl.get().hasBody()) {
    // // // return Optional.of(functionDecl);
    // // return functionDecl;
    // // }
    // //
    // // // Search for the declaration
    // // return getAppTry().flatMap(app -> app.getFunctionDeclaration(functionDecl.get().getDeclName(),
    // // functionDecl.get().getFunctionType()));
    // }

    /**
     * The FunctionDecl as given by Clang. Usually it is the first that appears in the code.
     * 
     * @return
     */
    public Optional<FunctionDecl> getFunctionDecl() {

        // TODO: Replace with get(DIRECT_CALLEE) when refactoring to new format is complete
        return get(DIRECT_CALLEE)
                // If FunctionDecl has a primary template decl (e.g., is a template specialization), return original
                // template instead
                .map(ClavaNodes::normalizeDecl)
                .map(FunctionDecl.class::cast);
        // .flatMap(fDecl -> fDecl.hasValue(FunctionDecl.PRIMARY_TEMPLATE_DECL)
        // ? fDecl.get(FunctionDecl.PRIMARY_TEMPLATE_DECL)
        // : Optional.of(fDecl));

        // Decl calleeDecl = get(DIRECT_CALLEE);
        // return calleeDecl instanceof FunctionDecl ? Optional.of((FunctionDecl) calleeDecl) : Optional.empty();
        // return (FunctionDecl) get(CALLEE_DECL);

        // DeclRefExpr declRef = getCalleeDeclRefTry().orElse(null);
        //
        // if (declRef == null) {
        // return Optional.empty();
        // }
        //
        // Optional<DeclaratorDecl> varDecl = declRef.getVariableDeclaration();
        // // Optional<DeclaratorDecl> varDecl = getCalleeDeclRef().getVariableDeclaration();
        //
        // if (!varDecl.isPresent()) {
        // return Optional.empty();
        // }
        //
        // DeclaratorDecl declarator = varDecl.get();
        // if (declarator instanceof FunctionDecl) {
        // return Optional.of((FunctionDecl) declarator);
        // }
        //
        // // E.g., constructors
        // /*
        // if (declarator instanceof VarDecl) {
        // System.out.println("VarDecl Type:" + declarator.getType());
        // Expr initExpr = ((VarDecl) declarator).getInit().orElse(null);
        // if (initExpr == null) {
        // SpecsLogs.msgWarn("Could not extract function from call from VarDecl, check if ok:\n" + declarator);
        // return Optional.empty();
        // }
        //
        // if (initExpr instanceof CXXConstructExpr) {
        // Type initExprType = initExpr.getType();
        // RecordType recordType = initExprType instanceof RecordType ? (RecordType) initExprType
        // : initExprType.desugarTo(RecordType.class);
        //
        // // RecordType recordType = initExpr.getType().desugarTo(RecordType.class);
        // CXXRecordDecl recordDecl = getApp().getCXXRecordDeclTry(recordType).orElse(null);
        // if (recordDecl == null) {
        // return Optional.empty();
        // }
        // System.out.println("RECORD DECL:" + recordDecl);
        // // recordType.getDeclInfo().;
        // System.out.println("Constructor type:" + initExpr.getType());
        // }
        // }
        // */
        // SpecsLogs.msgLib("Could not extract function from call callee decl, check if ok:\n" + declarator);
        // return Optional.empty();

        // if (!(declarator instanceof FunctionDecl)) {
        // SpecsLogs.msgWarn("Call callee decl is not a function decl, check if ok:\n" + declarator);
        // return Optional.empty();
        // }
        //
        // return Optional.of((FunctionDecl) declarator);
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

        // throw new RuntimeException("Could not find a node that implements the interface 'Nameable':" + getCallee());

        // SpecsLogs.debug(() -> "Could not find callee name for node:" + getCallee());
        return Optional.empty();

        /*
        // Try DeclRef
        Optional<DeclRefExpr> declRefExpr = getCalleeDeclRefTry();
        if (declRefExpr.isPresent()) {
            return declRefExpr.get().getRefName();
        }
        
        // Special case: UnresolvedLookupExpr
        Optional<UnresolvedLookupExpr> unresolvedLookup = getCallee()
                .getFirstDescendantsAndSelf(UnresolvedLookupExpr.class);
        if (unresolvedLookup.isPresent()) {
            return unresolvedLookup.get().getName();
        }
        */
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
        // System.out.println("CHANGING " + getCalleeName() + " to " + name);
        // System.out.println("FUNCTION BEFORE:\n" + getFunctionDecl().get().getCode());
        getCalleeDeclRef().setRefName(name);
        // System.out.println("FUNCTION AFTER:\n" + getFunctionDecl().get().getCode());
    }

    public Optional<FunctionType> getFunctionType() {

        var decls = getFunctionDecl().map(decl -> decl.getDecls()).orElse(Collections.emptyList());

        return decls.stream().map(FunctionDecl::getFunctionType).findFirst();

        // // First check declarations
        // FunctionType typeFromDecl = getPrototypes().stream()
        // .map(FunctionDecl::getFunctionType)
        // .findFirst()
        // .orElse(null);
        //
        // if (typeFromDecl != null) {
        // return Optional.of(typeFromDecl);
        // }
        //
        // // Check definition
        // FunctionType typeFromDef = getDefinition().map(FunctionDecl::getFunctionType).orElse(null);
        // if (typeFromDef != null) {
        // return Optional.of(typeFromDef);
        // }
        //
        // // Could not find the function type for call
        // return Optional.empty();
        // // throw new RuntimeException("Could not find the function type for call at " + getLocation());
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

    /**
     * Tries to return the signature of the corresponding function declaration. If not found, returns the signature of
     * the corresponding function definition. If not found, returns the name of the call.
     * 
     * <p>
     * As last resort returns simply the name of the call, because the arguments might not correspond to its signature
     * (e.g., printf).
     * 
     * @return
     */
    /*
    public String getSignature() {
        return getDeclaration().map(FunctionDecl::getSignature)
                .orElse(getDefinition().map(FunctionDecl::getSignature)
                        .orElse(getCalleeName()));
    }
    */

    /**
     * Returns the function declaration associated with this call.
     * 
     * <p>
     * No guarantees are made regarding if it is the declaration or definition of the function. First tries to return
     * the function definition, and if not found, the function declaration.
     * 
     * @return the function associated with this call
     */
    // public Optional<FunctionDecl> getFunction() {
    // Optional<FunctionDecl> definition = getDefinition();
    // if (definition.isPresent()) {
    // return definition;
    // }
    //
    // Optional<FunctionDecl> declaration = getDeclaration();
    // if (declaration.isPresent()) {
    // return declaration;
    // }
    //
    // return Optional.empty();
    // }

}
