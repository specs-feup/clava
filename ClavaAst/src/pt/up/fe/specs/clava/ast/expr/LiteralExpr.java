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
import pt.up.fe.specs.clava.ast.LiteralNode;

/**
 * Represents a literal piece of code corresponding to a statement.
 * 
 * @author JoaoBispo
 *
 */
public class LiteralExpr extends Expr implements LiteralNode {

    public LiteralExpr(DataStore nodeData, Collection<? extends ClavaNode> children) {
        super(nodeData, children);
    }

    @Override
    public String getCode() {
        return getLiteralCode();
    }

}
