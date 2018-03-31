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
import java.util.Collections;
import java.util.Optional;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.comment.InlineComment;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.VarDeclData;
import pt.up.fe.specs.clava.ast.decl.enums.InitializationStyle;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.expr.CXXConstructExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.InitListExpr;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsCollections;

/**
 * Represents a variable declaration or definition.
 * 
 * @author JoaoBispo
 *
 */
public class VarDecl extends DeclaratorDecl {

    private final VarDeclData data;

    public VarDecl(VarDeclData data, String varName, Type type, DeclData declData, ClavaNodeInfo info, Expr initExpr) {
        this(data, varName, type, declData, info, SpecsCollections.ofNullable(initExpr));
    }

    protected VarDecl(VarDeclData data, String varName, Type type, DeclData declData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(varName, type, declData, info, children);

        this.data = data;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new VarDecl(data.copy(), getDeclName(), getType(), getDeclData(), getInfo(), Collections.emptyList());
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        StorageClass storageClass = getVarDeclData().getStorageClass();
        if (storageClass != StorageClass.NONE) {
            code.append(getVarDeclData().getStorageClass().getString()).append(" ");
        }

        code.append(getType().getCode(getDeclNameCode()));
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

    private String getDeclNameCode() {
        String name = getDeclName();

        // if (!getVarDeclData().hasVarDeclDumperInfo()) {
        if (!getVarDeclData().hasVarDeclV2()) {
            return name;
        }

        // Check if it is a static member outside of the record
        if (getVarDeclData().isStaticDataMember()
                && getVarDeclData().isOutOfLine()) {
            name = getVarDeclData().getQualifiedName()
                    .orElseThrow(
                            () -> new RuntimeException("Expected qualified name to be defined for VarDecl.\n " + this));
        }

        return name;
    }

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

    private String listInitCode() {
        // Must be present
        Expr initList = getInit().get();
        SpecsCheck.checkArgument(initList instanceof InitListExpr,
                () -> "Expected argument to be an instance of " + InitListExpr.class + ", it is a "
                        + initList.getClass());

        return initList.getCode();
        // return getInit().map(ClavaNode::getCode)
        // .orElseThrow(() -> new RuntimeException());
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

    /**
     * Example: footype foo = <child_code>;
     * 
     * @return
     */
    private String cinitCode() {
        String prefix = " = ";

        // return " = " + getChild(0).getCode();

        Expr initExpr = getInit().get();

        // Special case: CXXConstructorExpr without args
        // if (initExpr instanceof CXXConstructExpr) {
        // if (((CXXConstructExpr) initExpr).getArgs().isEmpty()) {
        // return prefix + " {}";
        // }
        // }

        // Special case: InitListExpr
        // if (initExpr instanceof InitListExpr) {
        // InitListExpr initListExpr = (InitListExpr) initExpr;
        // List<Expr> initExprs = initListExpr.getInitExprs();
        //
        // if (initExprs.size() == 1) {
        // if (initExprs.get(0) instanceof InitListExpr) {
        // return prefix + initExprs.get(0).getCode();
        // }
        // }
        // }

        return prefix + initExpr.getCode();
    }

    private String callInitCode() {
        Preconditions.checkArgument(getNumChildren() == 1, "Expected one child");
        ClavaNode init = getChild(0);

        // If CXXConstructorExpr, use args code only
        if (init instanceof CXXConstructExpr) {
            return ((CXXConstructExpr) init).getArgsCode();
        }

        // If expression, just return the code
        Preconditions.checkArgument(init instanceof Expr,
                "Expected an Expr, got '" + init.getClass().getSimpleName() + "'");

        return "(" + init.getCode() + ")";

    }

    public VarDeclData getVarDeclData() {
        return data;
    }

    @Override
    public String toContentString() {
        return super.toContentString() + ", " + data;
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

        // No init, add child and set initialization method
        addChild(expression);
        data.setInitKind(InitializationStyle.CINIT);
    }

}
