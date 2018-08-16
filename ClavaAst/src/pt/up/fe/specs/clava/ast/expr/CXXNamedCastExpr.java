/**
 * Copyright 2017 SPeCS.
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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.extra.data.Language;

public abstract class CXXNamedCastExpr extends ExplicitCastExpr {

    /// DATAKEY BEGIN

    public final static DataKey<String> CAST_NAME = KeyFactory.string("castName");

    /// DATAKEY END

    public CXXNamedCastExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private final String castName;
    // private final String typeAsWritten;
    // private final CastKind castKind;

    // private final CXXNamedCastExprData cxxNamedCastExprdata;

    // public CXXNamedCastExpr(CXXNamedCastExprData cxxNamedCastExprdata, ExprData exprData, ClavaNodeInfo info,
    // Expr subExpr) {
    //
    // this(cxxNamedCastExprdata, exprData, info, Arrays.asList(subExpr));
    // }

    /**
     * General constructor.
     * 
     * @param castKind
     * @param location
     */
    // protected CXXNamedCastExpr(CXXNamedCastExprData cxxNamedCastExprdata, ExprData exprData, ClavaNodeInfo info,
    // List<? extends ClavaNode> children) {
    //
    // super(cxxNamedCastExprdata.getCastKind(), exprData, info, children);
    //
    // this.cxxNamedCastExprdata = cxxNamedCastExprdata;
    // }

    // public CXXNamedCastExprData getCxxNamedCastExprdata() {
    // return cxxNamedCastExprdata;
    // }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();
        // System.out.println("BEFORE:" + cxxNamedCastExprdata.getTypeAsWritten());
        // System.out.println("AFTER:" + getTypeCode());
        // HACK: To deal with _Bool in C++ files while it is not properly addressed
        // ExplicitCast has the attribute 'typeAsWritten', which should be used here instead of getType()
        // String typeCode = cxxNamedCastExprdata.getTypeAsWritten();
        String typeCode = get(TYPE_AS_WRITTEN).getCode(this);
        if (typeCode.equals("_Bool") && getAncestorTry(TranslationUnit.class)
                .map(tu -> tu.get(TranslationUnit.LANGUAGE).get(Language.C_PLUS_PLUS)).orElse(false)) {
            typeCode = "bool";
        }

        // String typeCode = getTypeCode();
        // System.out.println("SUB EXPR TYPE:" + getSubExpr().toTree());
        // System.out.println("VALUE DECL TYPE:" + ((DeclRefExpr) getSubExpr()).getValueDeclType());
        // System.out.println("TYPE CODE:" + typeCode);
        // System.out.println("TYPE AS WRITTEN:" + cxxNamedCastExprdata.getTypeAsWritten());
        // code.append(cxxNamedCastExprdata.getCastName());
        code.append(get(CAST_NAME));
        // code.append("<").append(cxxNamedCastExprdata.getTypeAsWritten()).append(">");
        code.append("<").append(typeCode).append(">");
        code.append("(").append(getSubExpr().getCode()).append(")");

        return code.toString();

    }
}
