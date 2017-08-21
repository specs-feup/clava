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
import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.language.CastKind;

/**
 * Represents an implicit type conversions which has no direct representation in the original source code.
 * 
 * @author JoaoBispo
 *
 */
public class CXXFunctionalCastExpr extends CastExpr {

    private final String targetType;

    public CXXFunctionalCastExpr(String targetType, CastKind castKind, ExprData exprData, ClavaNodeInfo info,
            Expr subExpr) {

        this(targetType, castKind, exprData, info, Arrays.asList(subExpr));

        // this.targetType = targetType;
    }

    /**
     * Constructor for node copy.
     * 
     * @param type
     * @param castKind
     * @param description
     * @param location
     */
    private CXXFunctionalCastExpr(String description, CastKind castKind, ExprData exprData, ClavaNodeInfo info,
            List<? extends ClavaNode> children) {

        super(castKind, exprData, info, children);

        this.targetType = description;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXFunctionalCastExpr(targetType, getCastKind(), getExprData(), getInfo(), Collections.emptyList());
    }

    @Override
    public String getCode() {
        return getType().getCode() + "(" + getSubExpr().getCode() + ")";
    }

    public String getTargetType() {
        return targetType;
    }
}
