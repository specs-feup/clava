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

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.LiteralNode;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.type.Type;

/**
 * Represents a literal piece of code corresponding to a statement.
 * 
 * @author JoaoBispo
 *
 */
public class LiteralExpr extends Expr implements LiteralNode {

    // private final String literalCode;

    public LiteralExpr(DataStore nodeData, Collection<? extends ClavaNode> children) {
        super(nodeData, children);
    }

    /**
     * Appends a semicolon if the given code does not end with one.
     * 
     * @param literalCode
     * @param type
     */
    // public LiteralExpr(String literalCode, Type type) {
    // this(literalCode, type, ClavaNodeInfo.undefinedInfo());
    // }
    //
    // private LiteralExpr(String literalCode, Type type, ClavaNodeInfo info) {
    // super(new ExprData(type), info, Collections.emptyList());
    //
    // this.literalCode = literalCode;
    // }

    /**
     * Legacy support.
     * 
     * @param type
     * @param info
     */
    protected LiteralExpr(Type type, ClavaNodeInfo info) {
        super(new ExprData(type), info, Collections.emptyList());
    }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new LiteralExpr(literalCode, getType(), getInfo());
    // }
    //
    // @Override
    // public String getCode() {
    // return literalCode;
    // }

}
