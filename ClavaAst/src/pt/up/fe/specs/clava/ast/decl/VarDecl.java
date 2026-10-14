/**
 * Copyright 2016 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.decl;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Interfaces.DataStore;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.comment.InlineComment;
import pt.up.fe.specs.clava.ast.decl.enums.InitializationStyle;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.TLSKind;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.collections.SpecsList;

import java.util.Collection;
import java.util.Optional;

/**
 * Represents a variable declaration or definition.
 *
 * @author JoaoBispo
 *
 */
public class VarDecl extends VarDeclBase {

    // Data declarations are generated from the wire schema in VarDeclBase.

    public VarDecl(DataStore data, Collection<? extends ClavaNode> children, VarDeclBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public VarDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {

        StringBuilder code = new StringBuilder();

        // Add storage class
        StorageClass storageClass = getWireStorageClass();
        if (storageClass != StorageClass.None) {
            code.append(getWireStorageClass().getString()).append(" ");
        }

        // Add attributes
        code.append(getAttributesCode());
        String declName = getDeclNameCode();

        // Type.getCode() accepts null if no name
        declName = declName.isEmpty() ? null : declName;

        code.append(getType().getCode(this, declName));
        code.append(getInitializationCode());

        return code.toString();
    }

    /**
     *
     * @return the code of the variable declaration, possibly with initialization, but without the type nor a comma (;)
     *         at the end
     */
    @Override
    public String getTypelessCode() {
        return getDeclNameCode() + getInitializationCode();
    }

    public String getInitializationCode() {

        if (hasInit()) {
            return getWireInitStyle().getCode(this);
        }

        return "";

    }

    /**
     *
     * @return the code related to the named decl, or null if no name
     */
    private String getDeclNameCode() {

        return getCurrentQualifiedName();
    }

    public boolean hasInit() {
        return hasChildren();
    }

    public Optional<Expr> getInit() {
        if (!hasChildren()) {
            return Optional.empty();
        }

        return Optional.of(getChild(Expr.class, 0));
    }

    @Override
    public void associateComment(InlineComment inlineComment) {
        super.associateComment(inlineComment);

        // If parent is a DeclStmt, move comment 'upward'
        if (!hasParent()) {
            return;
        }

        ClavaNode parent = getParent();
        if (!(parent instanceof DeclStmt)) {
            return;
        }

        removeInlineComments().stream()
                .forEach(parent::associateComment);

    }

    public void setInit(Expr expression) {
        // If has init, just replace expression
        if (hasInit()) {
            setChild(0, expression);
            return;
        }

        // No init before, add child
        addChild(expression);

    }

    public void removeInit(boolean removeConst) {
        // If no init, do nothing
        if (!hasInit()) {
            return;
        }

        // Remove child
        removeChild(0);

        // If element type is const, remove const
        if (removeConst) {
            var declType = getType();
            if (declType.isConst()) {
                // Copy
                var copy = (Type) declType.deepCopy();
                setType(copy);
                copy.removeConst();
            }

        }

    }

    /**
     * In most cases, returns this VarDecl. In case of declaration of global variables, returns the VarDecl
     * corresponding to its definition.
     *
     * @return the VarDecl that corresponds to its definition, or null if it could not be found
     */
    public VarDecl getDefinition() {
        // If no global storage, return itself
        if (!get(VarDecl.HAS_GLOBAL_STORAGE)) {
            return this;
        }

        switch (get(VarDecl.STORAGE_CLASS)) {
            // Is global and storageClass is 'none', this is the global definition
            case None:
                return this;
            case Extern:
                return getApp().getGlobalVarDefinition(this).orElse(null);
            default:
                SpecsLogs.warn("Case not contemplated yet: " + get(VarDecl.STORAGE_CLASS));
                return this;
        }

    }

    public void setStorageClass(String value) {
        // Get corresponding enum
        var storageClass = StorageClass.getHelper().fromValue(value);
        // Store it
        set(STORAGE_CLASS, storageClass);
    }

    public void setStorageClass(StorageClass storageClass) {
        set(STORAGE_CLASS, storageClass);
    }

    @Override
    public SpecsList<DataKey<?>> getSignatureKeys() {
        return super.getSignatureKeys().andAdd(STORAGE_CLASS);
    }

}
