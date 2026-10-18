/**
 * Copyright 2018 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * Describes an explicit type conversion that uses functional notion but could not be resolved because one or more
 * arguments are type-dependent.
 * 
 * @author JoaoBispo
 *
 */
public class CXXUnresolvedConstructExpr extends Expr {

    /// DATAKEYS BEGIN
    public static final DataKey<Boolean> IS_LIST_INITIALIZATION = KeyFactory.bool("isListInitialization");
    /// DATAKEYS END

    public CXXUnresolvedConstructExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public List<Expr> getArguments() {
        return getChildren(Expr.class);
    }

    @Override
    public String getCode() {
        if (get(IS_LIST_INITIALIZATION)) {
            List<Expr> arguments = getArguments();
            if (arguments.size() != 1 || !(arguments.get(0) instanceof InitListExpr)) {
                throw new IllegalArgumentException("List construction must contain one initializer list");
            }
            return get(TYPE).get().getCode(this) + arguments.get(0).getCode();
        }
        return get(TYPE).get().getCode(this) + "("
                + getArguments().stream().map(ClavaNode::getCode).collect(Collectors.joining(", ")) + ")";
    }

}
