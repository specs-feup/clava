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
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.Type;

/**
 * A C++ typeid expression (C++ [expr.typeid]), which gets the type_info that corresponds to the supplied type, or the
 * (possibly dynamic) type of the supplied expression
 * 
 * <p>
 * This represents code like typeid(int) or typeid(*objPtr)
 * 
 * @author JoaoBispo
 *
 */
public class CXXTypeidExpr extends CXXTypeidExprBase {

    // Data declarations are generated from the wire schema in CXXTypeidExprBase.

    public CXXTypeidExpr(DataStore data, Collection<? extends ClavaNode> children, CXXTypeidExprBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public CXXTypeidExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Optional<Expr> getOperandExpr() {
        return getWireIsTypeOperand() ? Optional.empty() : Optional.of((Expr) getWireOperand());
    }

    public Optional<Type> getOperandType() {
        return getWireIsTypeOperand() ? Optional.of((Type) getWireOperand()) : Optional.empty();
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append("typeid(");
        if (getWireIsTypeOperand()) {
            code.append(getOperandType().get().getCode(this));
        } else {
            code.append(getOperandExpr().get().getCode());
        }

        code.append(")");

        return code.toString();
    }
}
