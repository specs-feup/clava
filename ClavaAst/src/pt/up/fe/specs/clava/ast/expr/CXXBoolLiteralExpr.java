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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

public class CXXBoolLiteralExpr extends Expr {

    /// DATAKEYS BEGIN

    public static final DataKey<Boolean> VALUE = KeyFactory.bool("value");

    /// DATAKEYS END

    // private final boolean value;

    public CXXBoolLiteralExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public CXXBoolLiteralExpr(boolean value, ExprData exprData, ClavaNodeInfo info) {
        this(new LegacyToDataStore().setNodeInfo(info).setExpr(exprData).getData(), Collections.emptyList());
        // this(value, exprData, info, Collections.emptyList());

        getDataI().add(VALUE, value);
    }

    // private CXXBoolLiteralExpr(boolean value, ExprData exprData, ClavaNodeInfo info,
    // Collection<? extends ClavaNode> children) {
    // super(exprData, info, children);
    //
    // this.value = value;
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new CXXBoolLiteralExpr(value, getExprData(), getInfo(), Collections.emptyList());
    // }

    @Override
    public String getCode() {
        return Boolean.toString(get(VALUE));
    }

}
