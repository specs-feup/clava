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
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.decl.DeclaratorDecl;
import pt.up.fe.specs.clava.ast.decl.EnumConstantDecl;
import pt.up.fe.specs.clava.ast.decl.ValueDecl;
import pt.up.fe.specs.clava.ast.expr.enums.DeclRefKind;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorKind;
import pt.up.fe.specs.clava.language.CXXOperator;
import pt.up.fe.specs.clava.utils.Nameable;
import pt.up.fe.specs.util.exceptions.CaseNotDefinedException;

/**
 * Represents a reference to a declaration (e.g., variable, function...).
 * 
 * @author JoaoBispo
 *
 */
public class DeclRefExpr extends Expr implements Nameable {

    // DATAKEY BEGIN

    /**
     * The nested-name qualifier that precedes the name, or empty string if it has none.
     */
    public final static DataKey<String> QUALIFIER = KeyFactory.string("qualifier");

    public final static DataKey<List<String>> TEMPLATE_ARGUMENTS = KeyFactory
            .generic("templateArguments", (List<String>) new ArrayList<String>())
            .setDefault(() -> new ArrayList<>());

    // public final static DataKey<String> DECL_NAME = KeyFactory.string("declName");

    // public final static DataKey<String> DECL_ID = KeyFactory.string("declId");

    public final static DataKey<ValueDecl> DECL = KeyFactory.object("decl", ValueDecl.class);

    public final static DataKey<String> CUSTOM_NAME = KeyFactory.string("customName");

    // DATAKEY END

    // private String customName;

    // private final String qualifier;
    // private final List<String> templateArguments;
    // private final BareDeclData declData;

    // private boolean hasUniqueDeclaration;
    // private final BareDeclData foundDeclData;

    public DeclRefExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

        // this.customName = null;

        // qualifier = data.get(QUALIFIER);
        // templateArguments = get(TEMPLATE_ARGUMENTS);
        // declData = null;
        // foundDeclData = null;
        // hasUniqueDeclaration = false;
        // System.out.println("DECL REF CHILDREN:" + children);
    }

    // public ValueDecl getDeclaration() {
    // ClavaNode child = getChild(0);
    //
    // if (child instanceof ValueDecl) {
    // return (ValueDecl) child;
    // }
    //
    // return getFactory().dummyValueDecl("dummy decl name", getFactory().dummyType("dummy type"));
    // }

    /**
     * A version of the declaration that is "owned" by this node. Can set the parameters of the declaration without
     * changing other nodes.
     * 
     * @return
     */
    /*
    private ValueDecl getUniqueDeclaration() {
        if (hasUniqueDeclaration) {
            return getChild(ValueDecl.class, 0);
        }
    
        // Copy current declaration
        ValueDecl declarationCopy = (ValueDecl) getDeclaration().copy();
    
        // Replace declaration
        setChild(0, declarationCopy);
    
        // Set flag
        hasUniqueDeclaration = true;
    
        return declarationCopy;
    }
    */

    // public NamedDecl getFoundDeclaration() {
    // return getChild(NamedDecl.class, 1);
    // }

    // public DeclRefExpr(String qualifier, List<String> templateArguments, BareDeclData declData,
    // BareDeclData foundDeclData,
    // ExprData exprData, ClavaNodeInfo info) {
    //
    // this(new LegacyToDataStore().setExpr(exprData).setNodeInfo(info).getData(), Collections.emptyList());
    // // super(exprData, info, Collections.emptyList());
    //
    // set(QUALIFIER, qualifier);
    // set(TEMPLATE_ARGUMENTS, templateArguments);
    // // this.qualifier = qualifier;
    // // this.templateArguments = templateArguments;
    // // this.declData = declData;
    // // this.foundDeclData = foundDeclData;
    //
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new DeclRefExpr(qualifier, new ArrayList<>(templateArguments), declData.copy(), foundDeclData,
    // getExprData(),
    // getInfo());
    // }

    @Override
    public String getCode() {
        String refName = getRefName();

        // Case operator
        Optional<CXXOperator> operator = CXXOperator.parseTry(refName);
        if (operator.isPresent()) {
            return operator.get().getString();
        }
        // if (refName.startsWith("operator")) {
        // return refName.substring("operator".length());
        // }

        String refNameSuffix = "";

        // Check if it has template arguments
        List<String> templateArguments = get(TEMPLATE_ARGUMENTS);
        if (!templateArguments.isEmpty()) {
            String templateArgs = templateArguments.stream().collect(Collectors.joining(", "));

            refNameSuffix = "<" + templateArgs + ">";
        }

        String qualifier = get(QUALIFIER);
        String refnamePrefix = qualifier == null ? "" : qualifier;

        // HACK:
        if (refnamePrefix.startsWith("class ")) {
            refnamePrefix = refnamePrefix.substring("class ".length());
        }

        return refnamePrefix + refName + refNameSuffix;

    }

    // @Override
    // public String toContentString() {
    // StringBuilder builder = new StringBuilder();
    // builder.append(super.toContentString());
    // builder.append(", refType:" + declData.getValueDeclType());
    // builder.append(", refName:" + getRefName());
    //
    // return builder.toString();
    // }

    // public String getValueDeclType() {
    // return getDeclaration().getType();
    // // return declData.getValueDeclType().get(0);
    // }

    public String getRefName() {
        if (hasValue(CUSTOM_NAME)) {
            return get(CUSTOM_NAME);
        }
        // if (customName != null) {
        // return customName;
        // }

        return get(DECL).get(ValueDecl.DECL_NAME);
        // return get(DECL_NAME);
        // return getDeclaration().getDeclName();
        // return declData.getDeclName();
    }

    public void setRefName(String refName) {
        set(CUSTOM_NAME, refName);
        // this.customName = refName;
        // set(DECL_NAME, refName);
        // getUniqueDeclaration().setDeclName(refName);
        // declData.setDeclName(refName);
    }

    /**
     * 
     * @return
     */

    // public Optional<? extends Decl> getDeclaration() {
    public ValueDecl getDeclaration() {
        return get(DECL);
        // return Optional.of(get(DECL));
        /*
        // If no id, return
        // if (!getInfo().getId().isPresent()) {
        if (!getExtendedId().isPresent()) {
            return Optional.empty();
        }
        
        Optional<String> idSuffix = getIdSuffix();
        
        if (!idSuffix.isPresent()) {
            throw new RuntimeException("Could not find id suffix in '" + getExtendedId() + "'");
        }
        // System.out.println("DECL DATA:" + declData);
        // System.out.println("ID SUFFIX:" + idSuffix);
        // String varDeclId = "0x" + Long.toHexString(declData.getPointer()) + idSuffix.get();
        String varDeclId = get(DECL_ID);
        Optional<ClavaNode> declTry = getApp().getNodeTry(varDeclId);
        
        // If not present, probably declaration is outside of parsed files
        // (e.g., declaration of a function in a system header, such as printf)
        if (!declTry.isPresent()) {
            return Optional.empty();
        }
        
        // ClavaNode decl = getApp().getNodeTry(varDeclId).orElseThrow(() -> new RuntimeException(
        // "Could not find declaration with id '" + varDeclId + "' for DeclRefExpr:" + this));
        
        ClavaNode decl = declTry.get();
        
        if (decl instanceof Decl) {
            return Optional.of((Decl) decl);
        }
        
        if (decl instanceof DeclStmt) {
            List<VarDecl> varDecls = ((DeclStmt) decl).getVarDecls();
        
            return varDecls.stream().filter(varDecl -> varDecl.getDeclName().equals(getRefName()))
                    .map(DeclaratorDecl.class::cast)
                    .findFirst();
        }
        
        throw new RuntimeException("Could not find the declaration associated with this DeclRefExpr:" + this);
        */
    }

    public Optional<DeclaratorDecl> getVariableDeclaration() {

        ValueDecl decl = getDeclaration();

        /*
        Optional<? extends Decl> declTry = getDeclaration();
        
        if (!declTry.isPresent()) {
            return Optional.empty();
        }
        
        Decl decl = declTry.get();
        */

        // Decl decl = getDeclaration();

        /*
        Optional<String> idSuffix = getInfo().getIdSuffix();
        if (!idSuffix.isPresent()) {
            // throw new RuntimeException("Could not find id suffix in '" + getExtendedId() + "'");
            return Optional.empty();
        }
        
        String varDeclId = "0x" + Long.toHexString(declData.getPointer()) + idSuffix.get();
        
        ClavaNode decl = getApp().getNodeTry(varDeclId).orElse(null);
        
        if (decl == null) {
            return Optional.empty();
        }
        */

        if (decl instanceof DeclaratorDecl) {
            return Optional.of((DeclaratorDecl) decl);
        }
        /*
        if (decl instanceof DeclStmt) {
            List<VarDecl> varDecls = ((DeclStmt) decl).getVarDecls();
        
            return varDecls.stream().filter(varDecl -> varDecl.getDeclName().equals(getRefName()))
                    .map(DeclaratorDecl.class::cast)
                    .findFirst();
        }
        */
        // Not a declarator, but a value decl
        if (decl instanceof EnumConstantDecl) {
            return Optional.empty();
        }

        throw new RuntimeException("Not implemented for " + decl.getClass());

        /*
        String refName = getRefName();
        
        Optional<Stmt> currentScope = getAncestorTry(Stmt.class);
        
        while (currentScope.isPresent()) {
            // Search in the children of the stmt
            Optional<VarDecl> refVarDecl = currentScope.get().getChildrenStream()
                    .filter(child -> child instanceof VarDecl && ((VarDecl) child).hasDeclName())
                    .map(child -> (VarDecl) child)
                    .filter(vardecl -> vardecl.getDeclName().equals(refName))
                    .findFirst();
        
            if (refVarDecl.isPresent()) {
                return refVarDecl.get();
            }
        
            currentScope = currentScope.get().getAncestorTry(Stmt.class);
        }
        
        LoggingUtils.msgInfo(
                "Could not find the variable declaration of the reference '" + refName + "' (" + getLocation() + ")");
        
        return null;
        */
    }

    public boolean isFunctionCall() {
        ClavaNode parent = ClavaNodes.getParentNormalized(this);

        return isFunctionCall(parent);
    }

    private boolean isFunctionCall(ClavaNode parent) {
        // Check if directly below a call
        if (!(parent instanceof CallExpr)) {
            return false;
        }

        // Check if on the callee part
        CallExpr callExpr = (CallExpr) parent;

        return callExpr.getCallee().getDescendantsAndSelfStream()
                .filter(descendant -> descendant == this)
                .findFirst().isPresent();
    }

    public boolean isArrayAccess() {
        ClavaNode parent = ClavaNodes.getParentNormalized(this);

        return isArrayAccess(parent);
    }

    private boolean isArrayAccess(ClavaNode parent) {

        // Check if parent is an ArraySubscript
        if (!(parent instanceof ArraySubscriptExpr)) {
            return false;
        }

        // If first child, declRef is an array access
        ClavaNode normalizedFirstChild = ClavaNodes.normalize(parent.getChild(0));

        return normalizedFirstChild == this;
    }

    private ArraySubscriptExpr getArrayAccessExpr(DeclRefExpr arrayName) {
        ClavaNode currentParent = ClavaNodes.getParentNormalized(arrayName);
        Preconditions.checkArgument(currentParent instanceof ArraySubscriptExpr);

        while (true) {
            // While parent is an array subscript, fecth parent
            ClavaNode newParent = ClavaNodes.getParentNormalized(currentParent);

            if (newParent instanceof ArraySubscriptExpr) {
                currentParent = newParent;
                continue;
            } else {
                return (ArraySubscriptExpr) currentParent;
            }
        }

    }

    /*
    private boolean isVarAccess(ClavaNode parent) {
        // Check if parent is a BinaryOperator
        if (!(parent instanceof BinaryOperator)) {
            return false;
        }
    
        BinaryOperator op = (BinaryOperator) parent;
    
        // Check if assign op
        if (!(op.getOp() == BinaryOperatorKind.ASSIGN)) {
            return false;
        }
    
        // Check if this node is on the left-hand side of the operator
        boolean isOnLhs = op.getLhs().getDescendantsAndSelfStream()
                // Check if it is the same node
                .filter(descendant -> descendant == this)
                .findFirst()
                .isPresent();
    
        return isOnLhs;
    }
    */

    private DeclRefKind isUnaryOperator(ClavaNode parent) {
        // Check if parent is a UnaryOperator
        if (!(parent instanceof UnaryOperator)) {
            return null;
        }

        UnaryOperator op = (UnaryOperator) parent;

        // Check if deref op
        if (op.getOp() == UnaryOperatorKind.DEREF) {
            return DeclRefKind.POINTER_ACCESS;
        }

        if (op.getOp() == UnaryOperatorKind.ADDR_OF) {
            return DeclRefKind.ADDRESS_OF;
        }

        return null;
    }

    public DeclRefKind getKind() {
        ClavaNode parent = ClavaNodes.getParentNormalized(this);

        // Check var access
        // if (isVarAccess(parent)) {
        // return DeclRefKind.VAR_ACCESS;
        // }

        if (isArrayAccess(parent)) {
            return DeclRefKind.ARRAY_ACCESS;
        }

        DeclRefKind unaryKind = isUnaryOperator(parent);
        if (unaryKind != null) {
            return unaryKind;
        }

        if (isFunctionCall(parent)) {
            return DeclRefKind.FUNCTION_CALL;
        }

        return DeclRefKind.VAR_ACCESS;

    }

    public Expr getUseExpr() {
        DeclRefKind kind = getKind();

        switch (kind) {
        case VAR_ACCESS:
            return this;
        case ARRAY_ACCESS:
            return getArrayAccessExpr(this);
        case FUNCTION_CALL:
        case POINTER_ACCESS:
        case ADDRESS_OF:
            return (Expr) ClavaNodes.getParentNormalized(this);
        case UNKNOWN:
            return this;
        default:
            throw new CaseNotDefinedException(kind);
        }

    }

    @Override
    public String getName() {
        return getRefName();
    }

    @Override
    public void setName(String name) {
        setRefName(name);
    }
}
