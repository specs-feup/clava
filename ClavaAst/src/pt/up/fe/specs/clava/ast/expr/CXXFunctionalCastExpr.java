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
import java.util.List;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.language.CastKind;

/**
 * Represents an implicit type conversions which has no direct representation in the original source code.
 * 
 * @author JoaoBispo
 *
 */
public class CXXFunctionalCastExpr extends CastExpr {

    /// DATAKEYS BEGIN

    // public final static DataKey<Type>

    /// DATAKEYS END

    // private final String targetType;

    public CXXFunctionalCastExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public CXXFunctionalCastExpr(CastKind castKind, ExprData exprData, ClavaNodeInfo info,
            Expr subExpr) {

        this(castKind, exprData, info, Arrays.asList(subExpr));

        // this.targetType = targetType;
    }

    public static CXXFunctionalCastExpr newInstance(CastExpr cast, Expr subExpr) {
        return new CXXFunctionalCastExpr(cast.getCastKind(), cast.getExprData(), cast.getInfo(),
                subExpr);

        // if (cast.hasDataI()) {
        // throw new RuntimeException("Not implemented for ClavaData nodes");
        // }
        //
        // return new CXXFunctionalCastExpr(cast.getCastKind(), cast.getExprData(), cast.getInfo(),
        // subExpr);
    }

    /**
     * Constructor for node copy.
     * 
     * @param type
     * @param castKind
     * @param description
     * @param location
     */
    private CXXFunctionalCastExpr(CastKind castKind, ExprData exprData, ClavaNodeInfo info,
            List<? extends ClavaNode> children) {

        super(new LegacyToDataStore().setExpr(exprData).setNodeInfo(info).getData(), children);

        put(CastExpr.CAST_KIND, castKind);
        // super(castKind, exprData, info, children);

        // this.targetType = description;
    }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new CXXFunctionalCastExpr(getCastKind(), getExprData(), getInfo(), Collections.emptyList());
    // }

    @Override
    public String getCode() {
        return getType().getCode() + "(" + getSubExpr().getCode() + ")";
    }

    // public String getTargetType() {
    // return targetType;
    // }
}
