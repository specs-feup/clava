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

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class GNUNullExpr extends Expr {

    public GNUNullExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public GNUNullExpr(ExprData exprData, ClavaNodeInfo info) {
    // super(exprData, info, Collections.emptyList());
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new GNUNullExpr(getExprData(), getInfo());
    // }

    @Override
    public String getCode() {
        // Using 0 instead of NULL so that the same code is generated, wheter we use Clang of Gcc
        return "0";
        // return "NULL";
    }
}
