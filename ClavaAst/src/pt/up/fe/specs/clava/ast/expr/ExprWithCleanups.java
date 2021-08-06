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
 * Represents an expression that introduces cleanups to be run at the end of the sub-expression's evaluation. The most
 * common source of expression-introduced cleanups is temporary objects in C++.
 * 
 * <p>
 * This expression also tracks whether the sub-expression contains a potentially-evaluated block literal. The lifetime
 * of a block literal is the extent of the enclosing scope.
 * 
 * @author JoaoBispo
 *
 */
public class ExprWithCleanups extends FullExpr {

    public ExprWithCleanups(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

}
