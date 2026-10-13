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

import pt.up.fe.specs.clava.ClavaNode;

/**
 * Describes an explicit type conversion that uses functional notion but could not be resolved because one or more
 * arguments are type-dependent.
 * 
 * @author JoaoBispo
 *
 */
public class CXXUnresolvedConstructExpr extends CXXUnresolvedConstructExprBase {

    // Data declarations are generated from the wire schema in CXXUnresolvedConstructExprBase.

    public CXXUnresolvedConstructExpr(DataStore data, Collection<? extends ClavaNode> children, CXXUnresolvedConstructExprBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public CXXUnresolvedConstructExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public List<Expr> getArguments() {
        return getChildren(Expr.class);
    }

    @Override
    public String getCode() {
        if (getWireIsListInitialization()) {
            List<Expr> arguments = getArguments();
            if (arguments.size() != 1 || !(arguments.get(0) instanceof InitListExpr)) {
                throw new IllegalArgumentException("List construction must contain one initializer list");
            }
            return getWireType().get().getCode(this) + arguments.get(0).getCode();
        }
        return getWireType().get().getCode(this) + "("
                + getArguments().stream().map(ClavaNode::getCode).collect(Collectors.joining(", ")) + ")";
    }

}
