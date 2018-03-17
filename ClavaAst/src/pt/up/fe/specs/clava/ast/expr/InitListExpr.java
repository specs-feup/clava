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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.data.InitListExprData;
import pt.up.fe.specs.util.SpecsLogs;

/**
 * C or C++ initializer list.
 * 
 * @author JoaoBispo
 *
 */
public class InitListExpr extends Expr {

    private final InitListExprData data;

    public InitListExpr(InitListExprData data, ExprData exprData, ClavaNodeInfo info,
            Collection<? extends Expr> initExprs) {

        super(exprData, info, initExprs);

        this.data = data;
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
        // return new InitListExpr(hasInitializedFieldInUnion, arrayFiller, fieldData, getExprData(), getInfo(),
        // Collections.emptyList());
        return new InitListExpr(data, getExprData(), getInfo(),
                Collections.emptyList());
    }

    public List<Expr> getInitExprs() {
        return getChildren(Expr.class);
    }

    @Override
    public String getCode() {
        String list = getInitExprs().stream()
                .map(expr -> expr.getCode())
                .collect(Collectors.joining(", "));

        // if (arrayFiller != null) {
        if (data.hasArrayFiller()) {
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

        String code = "{" + list + "}";

        // System.out.println("CODE:" + code);
        // System.out.println("INIT_LIST:" + this);
        // System.out.println("INIT_LIST TYPE:" + getType());
        return code;

        // if (data.isExplicit()) {
        // return "{" + list + "}";
        // } else {
        // return list;
        // }

        // , "{ ", " }"
        // return "{" + list + "}";
    }

    @Override
    public String toContentString() {
        return toContentString(super.toContentString(), data.toString());
    }
}
