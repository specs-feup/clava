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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
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
public class CXXTypeidExpr extends Expr {

    /// DATAKEYS BEGIN

    public final static DataKey<Boolean> IS_TYPE_OPERAND = KeyFactory.bool("typeOperand");

    public final static DataKey<ClavaNode> OPERAND = KeyFactory.object("operand", ClavaNode.class);

    /// DATAKEYS END

    public CXXTypeidExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Optional<Expr> getOperandExpr() {
        return get(IS_TYPE_OPERAND) ? Optional.empty() : Optional.of((Expr) get(OPERAND));
    }

    public Optional<Type> getOperandType() {
        return get(IS_TYPE_OPERAND) ? Optional.of((Type) get(OPERAND)) : Optional.empty();
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append("typeid(");
        if (get(IS_TYPE_OPERAND)) {
            code.append(getOperandType().get().getCode(this));
        } else {
            code.append(getOperandExpr().get().getCode());
        }

        code.append(")");

        return code.toString();
    }
}
