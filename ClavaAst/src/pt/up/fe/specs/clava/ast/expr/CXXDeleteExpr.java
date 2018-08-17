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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.util.SpecsLogs;

public class CXXDeleteExpr extends Expr {

    /// DATAKEYS BEGIN

    public final static DataKey<Boolean> IS_GLOBAL = KeyFactory.bool("isGlobal");
    public final static DataKey<Boolean> IS_ARRAY = KeyFactory.bool("isArray");
    public final static DataKey<Boolean> IS_ARRAY_AS_WRITTEN = KeyFactory.bool("isArrayAsWritten");
    // public final static DataKey<Expr> ARGUMENT_EXPR = KeyFactory.object("argumentExpr", Expr.class);

    public CXXDeleteExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private final boolean isGlobal;
    // private final boolean isArray;
    // private final BareDeclData operatorDelete;
    //
    // public CXXDeleteExpr(boolean isGlobal, boolean isArray, BareDeclData operatorDelete, ExprData exprData,
    // ClavaNodeInfo info, Expr argument) {
    //
    // this(isGlobal, isArray, operatorDelete, exprData, info, Arrays.asList(argument));
    // }
    //
    // private CXXDeleteExpr(boolean isGlobal, boolean isArray, BareDeclData bareDecl, ExprData exprData,
    // ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    // super(exprData, info, children);
    //
    // this.isGlobal = isGlobal;
    // this.isArray = isArray;
    // this.operatorDelete = bareDecl;
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new CXXDeleteExpr(isGlobal, isArray, operatorDelete, getExprData(), getInfo(),
    // Collections.emptyList());
    // }

    public Expr getArgument() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        if (get(IS_GLOBAL)) {
            SpecsLogs.msgWarn("Code generation not implemented yet when global is true");
        }

        code.append("delete");
        if (get(IS_ARRAY_AS_WRITTEN)) {
            code.append("[]");
        }

        Expr arg = getArgument();
        if (!arg.get(Expr.IS_DEFAULT_ARGUMENT)) {
            code.append(" ");
            code.append(getArgument().getCode());
        }

        return code.toString();
    }
}
