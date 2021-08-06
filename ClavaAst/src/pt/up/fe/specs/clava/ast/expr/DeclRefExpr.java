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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.decl.DeclaratorDecl;
import pt.up.fe.specs.clava.ast.decl.EnumConstantDecl;
import pt.up.fe.specs.clava.ast.decl.ValueDecl;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;
import pt.up.fe.specs.clava.ast.expr.enums.DeclRefKind;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorKind;
import pt.up.fe.specs.clava.language.CXXOperator;
import pt.up.fe.specs.clava.utils.Nameable;
import pt.up.fe.specs.util.collections.SpecsList;
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

    public final static DataKey<List<TemplateArgument>> TEMPLATE_ARGUMENTS = KeyFactory
            .generic("templateArguments", (List<TemplateArgument>) new ArrayList<TemplateArgument>())
            .setDefault(() -> new ArrayList<>());

    public final static DataKey<ValueDecl> DECL = KeyFactory.object("decl", ValueDecl.class);

    public final static DataKey<String> CUSTOM_NAME = KeyFactory.string("customName");

    // DATAKEY END

    public DeclRefExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

    }

    @Override
    public String getCode() {
        String refName = getRefName();

        // Case operator
        Optional<CXXOperator> operator = CXXOperator.parseTry(refName);
        if (operator.isPresent()) {
            return operator.get().getString();
        }

        String refNameSuffix = TemplateArgument.getCode(get(TEMPLATE_ARGUMENTS), this);

        String qualifier = get(QUALIFIER);
        String refnamePrefix = qualifier == null ? "" : qualifier;

        // HACK:
        if (refnamePrefix.startsWith("class ")) {
            refnamePrefix = refnamePrefix.substring("class ".length());
        }

        return refnamePrefix + refName + refNameSuffix;

    }

    public String getRefName() {
        if (hasValue(CUSTOM_NAME)) {
            return get(CUSTOM_NAME);
        }

        return getDeclaration().get(ValueDecl.DECL_NAME);
    }

    public void setRefName(String refName) {
        set(CUSTOM_NAME, refName);
    }

    /**
     * 
     * @return
     */

    public ValueDecl getDeclaration() {
        return (ValueDecl) ClavaNodes.normalizeDecl(get(DECL));
    }

    public Optional<DeclaratorDecl> getVariableDeclaration() {

        ValueDecl decl = getDeclaration();

        if (decl instanceof DeclaratorDecl) {
            return Optional.of((DeclaratorDecl) decl);
        }

        // Not a declarator, but a value decl
        if (decl instanceof EnumConstantDecl) {
            return Optional.empty();
        }

        throw new RuntimeException("Not implemented for " + decl.getClass());
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

    private DeclRefKind isUnaryOperator(ClavaNode parent) {
        // Check if parent is a UnaryOperator
        if (!(parent instanceof UnaryOperator)) {
            return null;
        }

        UnaryOperator op = (UnaryOperator) parent;

        // Check if deref op
        if (op.getOp() == UnaryOperatorKind.Deref) {
            return DeclRefKind.POINTER_ACCESS;
        }

        if (op.getOp() == UnaryOperatorKind.AddrOf) {
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

    @Override
    public SpecsList<DataKey<?>> getSignatureKeys() {
        return super.getSignatureKeys().andAdd(DECL);
    }

    @Override
    public SpecsList<String> getSignatureCustomStrings() {
        return super.getSignatureCustomStrings().andAdd(getRefName());
    }
}
