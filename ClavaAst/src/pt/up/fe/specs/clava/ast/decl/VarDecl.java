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
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.language.TLSKind;

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
            .setDefault(() -> StorageClass.NONE);

    public final static DataKey<TLSKind> TLS_KIND = KeyFactory
            .enumeration("tlsKind", TLSKind.class)
            .setDefault(() -> TLSKind.NONE);

    // public final static DataKey<Boolean> IS_MODULE_PRIVATE = KeyFactory.bool("isModulePrivate");

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

    // private final VarDeclData data;
    //
    // public VarDecl(VarDeclData data, String varName, Type type, DeclData declData, ClavaNodeInfo info, Expr initExpr)
    // {
    // this(data, varName, type, declData, info, SpecsCollections.ofNullable(initExpr));
    // }
    //
    // protected VarDecl(VarDeclData data, String varName, Type type, DeclData declData, ClavaNodeInfo info,
    // Collection<? extends ClavaNode> children) {
    //
    // super(varName, type, declData, info, children);
    //
    // this.data = data;
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new VarDecl(data.copy(), getDeclName(), getType(), getDeclData(), getInfo(), Collections.emptyList());
    // }

    @Override
    public String getCode() {
        // System.out.println("VARDECL TYPE:" + getType().toTree());
        // return getCode(null);
        // }

        /**
         * Overload which accepts an external decl name.
         * 
         * @param declName
         * @return
         */
        // public String getCode(String externalDeclName) {

        StringBuilder code = new StringBuilder();

        StorageClass storageClass = get(STORAGE_CLASS);
        if (storageClass != StorageClass.NONE) {
            code.append(get(STORAGE_CLASS).getString()).append(" ");
        }

        String declName = getDeclNameCode();
        // String declName = externalDeclName != null ? externalDeclName : getDeclNameCode();
        // System.out.println("DECL NAME:" + getDeclNameCode());
        // System.out.println("EXTERNAL DECL NAME:" + externalDeclName);
        // Type.getCode() accepts null if no name
        declName = declName.isEmpty() ? null : declName;

        code.append(getType().getCode(this, declName));
        code.append(getInitializationCode());

        // System.out.println("VARDECL CODE:" + code);
        // System.out.println("VARDECL TYPE:" + get(TYPE).toTree());
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

        // System.out.println("CHILDREN:" + getChildren());

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
        // if (!hasDeclName()) {
        // return null;
        // }

        String name = getDeclName();

        // if (!getVarDeclData().hasVarDeclDumperInfo()) {
        // if (!getVarDeclData().hasVarDeclV2()) {
        // return name;
        // }

        // Check if it is a static member outside of the record
        if (get(IS_STATIC_DATA_MEMBER) && get(IS_OUT_OF_LINE)) {
            // name = get(QUALIFIED_NAME);
            name = getQualifiedName();
        }

        return name;
    }

    /*
    public String getInitializationCode() {
        // Write code according to type of declaration
        switch (data.getInitKind()) {
        case CINIT:
            return cinitCode();
        case NO_INIT:
            return "";
        case CALL_INIT:
            return callInitCode();
        case LIST_INIT:
            return listInitCode();
        default:
            throw new RuntimeException("Case not defined:" + data.getInitKind());
        }
    }
    */

    // private String listInitCode() {
    // // Must be present
    // Expr initList = getInit().get();
    // SpecsCheck.checkArgument(initList instanceof InitListExpr,
    // () -> "Expected argument to be an instance of " + InitListExpr.class + ", it is a "
    // + initList.getClass());
    //
    // return initList.getCode();
    // // return getInit().map(ClavaNode::getCode)
    // // .orElseThrow(() -> new RuntimeException());
    // }

    public boolean hasInit() {
        return hasChildren();
    }

    public Optional<Expr> getInit() {
        if (!hasChildren()) {
            return Optional.empty();
        }

        return Optional.of(getChild(Expr.class, 0));
    }

    /**
     * Example: footype foo = <child_code>;
     * 
     * @return
     */
    // private String cinitCode() {
    // String prefix = " = ";
    //
    // // return " = " + getChild(0).getCode();
    //
    // Expr initExpr = getInit().get();
    //
    // // Special case: CXXConstructorExpr without args
    // // if (initExpr instanceof CXXConstructExpr) {
    // // if (((CXXConstructExpr) initExpr).getArgs().isEmpty()) {
    // // return prefix + " {}";
    // // }
    // // }
    //
    // // Special case: InitListExpr
    // // if (initExpr instanceof InitListExpr) {
    // // InitListExpr initListExpr = (InitListExpr) initExpr;
    // // List<Expr> initExprs = initListExpr.getInitExprs();
    // //
    // // if (initExprs.size() == 1) {
    // // if (initExprs.get(0) instanceof InitListExpr) {
    // // return prefix + initExprs.get(0).getCode();
    // // }
    // // }
    // // }
    //
    // return prefix + initExpr.getCode();
    // }

    // private String callInitCode() {
    // Preconditions.checkArgument(getNumChildren() == 1, "Expected one child");
    // ClavaNode init = getChild(0);
    //
    // // If CXXConstructorExpr, use args code only
    // if (init instanceof CXXConstructExpr) {
    // return ((CXXConstructExpr) init).getArgsCode();
    // }
    //
    // // If expression, just return the code
    // Preconditions.checkArgument(init instanceof Expr,
    // "Expected an Expr, got '" + init.getClass().getSimpleName() + "'");
    //
    // return "(" + init.getCode() + ")";
    //
    // }

    // public VarDeclData getVarDeclData() {
    // return data;
    // }

    // @Override
    // public String toContentString() {
    // return super.toContentString() + ", " + data;
    // }

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

    /*
    public void setInit(Expr expression) {
        setInit(expression, InitializationStyle.CINIT);
    }
    
    public void setInit(Expr expression, InitializationStyle initStyle) {
        */

    public void setInit(Expr expression) {
        // If has init, just replace expression
        if (hasInit()) {
            setChild(0, expression);
            return;
        }

        // No init before, add child
        addChild(expression);

        // Set initialization method if current style is no initialization
        // if (get(INIT_STYLE) == InitializationStyle.NO_INIT) {
        // set(INIT_STYLE, InitializationStyle.CINIT);
        // }
        // set(INIT_STYLE, InitializationStyle.CINIT);
        // data.setInitKind(InitializationStyle.CINIT);
    }

}
