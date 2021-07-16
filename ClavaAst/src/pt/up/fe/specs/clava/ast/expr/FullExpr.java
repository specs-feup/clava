/**
 * Copyright 2021 SPeCS.
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

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.enums.ValueKind;

public abstract class FullExpr extends Expr {

    // public final static DataKey<Expr> SUB_EXPR = KeyFactory.object("subExpr", Expr.class);

    public FullExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // @Override
    // public String getCode() {
    // return get(SUB_EXPR).getCode();
    // }

    @Override
    public String getCode() {
        return getSubExpr().getCode();
    }

    public Expr getSubExpr() {
        return getChild(Expr.class, 0);
    }

    @Override
    public ValueKind getValueKind() {
        return getSubExpr().getValueKind();
    }
}
