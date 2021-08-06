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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.comment.InlineComment;
import pt.up.fe.specs.clava.ast.decl.enums.InitializationStyle;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.TLSKind;
import pt.up.fe.specs.util.SpecsLogs;

/**
 * Represents a variable declaration or definition.
 * 
 * @author JoaoBispo
 *
 */
public class VarDecl extends DeclaratorDecl {

    /// DATAKEYS BEGIN

    /**
     * The storage class as written in the source.
     */
    public final static DataKey<StorageClass> STORAGE_CLASS = KeyFactory
            .enumeration("storageClass", StorageClass.class)
            .setDefault(() -> StorageClass.None);

    public final static DataKey<TLSKind> TLS_KIND = KeyFactory
            .enumeration("tlsKind", TLSKind.class)
            .setDefault(() -> TLSKind.NONE);

    /**
     * True if this local variable can be used with the named return value optimization (NRVO).
     */
    public final static DataKey<Boolean> IS_NRVO_VARIABLE = KeyFactory.bool("isNRVOVariable");

    /**
     * The style of initialization for this declaration.
     * 
     * <p>
     * C-style initialization is "int x = 1;". Call-style initialization is a C++98 direct-initializer, e.g. "int
     * x(1);". The Init expression will be the expression inside the parens or a "ClassType(a,b,c)" class constructor
     * expression for class types. List-style initialization is C++11 syntax, e.g. "int x{1};". Clients can distinguish
     * between different forms of initialization by checking this value. In particular, "int x = {1};" is C-style, "int
     * x({1})" is call-style, and "int x{1};" is list-style; the Init expression in all three cases is an InitListExpr.
     */
    public final static DataKey<InitializationStyle> INIT_STYLE = KeyFactory
            .enumeration("initStyle", InitializationStyle.class)
            .setDefault(() -> InitializationStyle.CINIT);

    /**
     * True if this variable is (C++11) constexpr.
     */
    public final static DataKey<Boolean> IS_CONSTEXPR = KeyFactory.bool("isConstexpr");

    /**
     * True if this is a static data member.
     * 
     * <p>
     * This will only be true in C++, and applies to, e.g., the variable 'x' in:<br>
     * 
     * <pre>
     struct S {
         static int x;
     };
     * </pre>
     */
    public final static DataKey<Boolean> IS_STATIC_DATA_MEMBER = KeyFactory.bool("isStaticDataMember");

    /**
     * True if this is or was instantiated from an out-of-line definition of a static data member.
     */
    public final static DataKey<Boolean> IS_OUT_OF_LINE = KeyFactory.bool("isOutOfLine");

    /**
     * True if this variable does not have local storage.
     * 
     * <p>
     * This includes all global variables as well as static variables declared within a function.
     */
    public final static DataKey<Boolean> HAS_GLOBAL_STORAGE = KeyFactory.bool("hasGlobalStorage");

    /// DATAKEYS END

    public VarDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {

        StringBuilder code = new StringBuilder();

        // Add storage class
        StorageClass storageClass = get(STORAGE_CLASS);
        if (storageClass != StorageClass.None) {
            code.append(get(STORAGE_CLASS).getString()).append(" ");
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
            return get(INIT_STYLE).getCode(this);
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

    public void removeInit() {
        // If no init, do nothing
        if (!hasInit()) {
            return;
        }

        // Remove child
        removeChild(0);

        // If element type is const, remove const
        var declType = getType();
        if (declType.isConst()) {
            // Copy
            var copy = (Type) declType.deepCopy();
            setType(copy);
            copy.removeConst();
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
        // Is global and storageClass is 'none', this is the global declaration
        case None:
            return this;
        case Extern:
            return getApp().getGlobalVarDefinition(this).orElse(null);
        default:
            SpecsLogs.warn("Case not contemplated yet: " + get(VarDecl.STORAGE_CLASS));
            return this;
        }

    }

    public static VarDecl getGlobalDefinition(App app, VarDecl varDecl) {
        return app.getDescendantsStream()
                .filter(node -> node instanceof VarDecl)
                .map(node -> (VarDecl) node)
                .filter(vardecl -> vardecl.get(DECL_NAME).equals(varDecl.get(DECL_NAME)))
                .filter(vardecl -> vardecl.get(STORAGE_CLASS) == StorageClass.None)
                .findFirst()
                .orElse(null);
    }
}
