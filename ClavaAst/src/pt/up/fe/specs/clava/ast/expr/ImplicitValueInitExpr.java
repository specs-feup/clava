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

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * Represents an implicit type conversions which has no direct representation in the original source code.
 * 
 * @author JoaoBispo
 *
 */
public class ImplicitValueInitExpr extends Expr {

    public ImplicitValueInitExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // @Override
    // public String getCode() {
    // // Return a default value initialization, based on the type
    // System.out.println("TYPE: " + getType());
    // return "/*implicit*/";
    // // return super.getCode();
    // }
    // public ImplicitValueInitExpr(ExprData exprData, ClavaNodeInfo info) {
    // this(exprData, info, Collections.emptyList());
    // }

    /**
     * Constructor for node copy.
     * 
     * @param castKind
     * @param destinationTypes
     * @param location
     */
    // private ImplicitValueInitExpr(ExprData exprData, ClavaNodeInfo info,
    // List<? extends ClavaNode> children) {
    //
    // super(exprData, info, children);
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new ImplicitValueInitExpr(getExprData(), getInfo(), Collections.emptyList());
    // }

    // @Override
    // public String toContentString() {
    // return super.toContentString() + " (type:" + getExprType().getCode(this) + ")";
    // }

}
