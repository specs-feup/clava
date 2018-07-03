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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.FieldDecl;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.data.InitListExprData;
import pt.up.fe.specs.clava.ast.type.ConstantArrayType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsLogs;

/**
 * C or C++ initializer list.
 * 
 * @author JoaoBispo
 *
 */
public class InitListExpr extends Expr {

    /// DATAKEY BEGIN

    /**
     * If this initializer list initializes an array with more elements than there are initializers in the list,
     * specifies an expression to be used for value initialization of the rest of the elements.
     */
    public final static DataKey<Expr> ARRAY_FILLER = KeyFactory.object("arrayFiller", Expr.class);
    public final static DataKey<FieldDecl> INITIALIZED_FIELD_IN_UNION = KeyFactory.object("initializedFieldInUnion",
            FieldDecl.class);

    public final static DataKey<Boolean> IS_EXPLICIT = KeyFactory.bool("isExplicit");

    public final static DataKey<Boolean> IS_STRING_LITERAL_INIT = KeyFactory.bool("isStringLiteralInit");

    /// DATAKEY END

    private boolean isOldFormat;

    private InitListExprData data;

    /*
    public InitListExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    
        isOldFormat = false;
        this.data = null;
    }
    */
    // @deprecated Cannot be replaced yet because of ArrayFiller, which class is not implemented yet in the new format

    /**
     * @param data
     * @param exprData
     * @param info
     * @param initExprs
     */
    @Deprecated
    public InitListExpr(InitListExprData data, ExprData exprData, ClavaNodeInfo info,
            Collection<? extends Expr> initExprs) {
        // this(new LegacyToDataStore().setExpr(exprData).setNodeInfo(info).getData(), initExprs);
        super(exprData, info, initExprs);

        this.data = data;
        isOldFormat = true;
    }

    /*
    private final boolean hasInitializedFieldInUnion;
    private final Expr arrayFiller;
    private final BareDeclData fieldData;
    
    public InitListExpr(boolean hasInitializedFieldInUnion, Expr arrayFiller, BareDeclData fieldData, ExprData exprData,
            ClavaNodeInfo info, Collection<? extends Expr> initExprs) {
    
        super(exprData, info, initExprs);
    
        this.hasInitializedFieldInUnion = hasInitializedFieldInUnion;
        this.arrayFiller = arrayFiller;
        this.fieldData = fieldData;
    }
    */

    @Override
    protected ClavaNode copyPrivate() {
        if (isOldFormat) {
            // return new InitListExpr(hasInitializedFieldInUnion, arrayFiller, fieldData, getExprData(), getInfo(),
            // Collections.emptyList());
            return new InitListExpr(data, getExprData(), getInfo(),
                    Collections.emptyList());
        }

        return super.copyPrivate();
    }

    public List<Expr> getInitExprs() {
        // return getChildren().stream().filter(child -> !(child instanceof Undefined))
        // .map(Expr.class::cast)
        // .collect(Collectors.toList());
        return getChildren(Expr.class);
    }

    public boolean hasArrayFiller() {
        return !(get(ARRAY_FILLER) instanceof NullExpr);
    }

    @Override
    public String getCode() {
        if (isOldFormat) {
            String list = getInitExprs().stream()
                    .map(expr -> expr.getCode())
                    .collect(Collectors.joining(", "));

            boolean hasSameInitsAsElements = hasSameInitsAsElements();

            // if (arrayFiller != null) {
            if (data.hasArrayFiller() && !hasSameInitsAsElements) {

                String exprClassName = data.getArrayFiller().getClass().getSimpleName();
                switch (exprClassName) {
                case "ImplicitValueInitExpr":
                    list = list + ",";
                    break;
                default:
                    SpecsLogs.msgWarn("Case not defined:" + exprClassName);
                    break;
                }
            }

            if (data.isExplicit()) {
                return "{" + list + "}";
            } else {
                return list;
            }
        }

        String list = getChildren().stream()
                .map(expr -> expr.getCode())
                .collect(Collectors.joining(", "));

        // if (arrayFiller != null) {
        Expr arrayFiller = get(ARRAY_FILLER);

        if (hasArrayFiller() && !get(IS_STRING_LITERAL_INIT)) {

            String exprClassName = arrayFiller.getClass().getSimpleName();
            switch (exprClassName) {
            case "ImplicitValueInitExpr":
                list = list + ",";
                break;
            default:
                SpecsLogs.msgWarn("Case not defined:" + exprClassName);
                break;
            }
        }

        if (get(IS_EXPLICIT)) {
            return "{" + list + "}";
        } else {
            return list;
        }

        /*
        if (data.hasArrayFiller() && !get(IS_STRING_LITERAL_INIT)) {
        
            String exprClassName = data.getArrayFiller().getClass().getSimpleName();
            switch (exprClassName) {
            case "ImplicitValueInitExpr":
                list = list + ",";
                break;
            default:
                SpecsLogs.msgWarn("Case not defined:" + exprClassName);
                break;
            }
        }
        
        if (data.isExplicit()) {
            return "{" + list + "}";
        } else {
            return list;
        }
        */
        // , "{ ", " }"
        // return "{" + list + "}";
    }

    private boolean hasSameInitsAsElements() {
        Type type = getType();
        if (!(type instanceof ConstantArrayType)) {
            return false;
        }

        ConstantArrayType constantArrayType = (ConstantArrayType) type;

        int arraySize = constantArrayType.getConstant();

        // Special case where initialization is a StringLiteral
        // TODO: Check if type of array is char?
        if (getInitExprs().size() == 1 && getInitExprs().get(0) instanceof StringLiteral) {
            StringLiteral stringLiteral = ((StringLiteral) getInitExprs().get(0));
            // Add one to StringLiteral due to \0
            return arraySize == stringLiteral.getStringContents().length() + 1;
        }

        return false;
    }

    @Override
    public String toContentString() {
        return toContentString(super.toContentString(), data.toString());
    }
}
