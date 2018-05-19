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

package pt.up.fe.specs.clava.ast.expr.data;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ast.type.Type;

public class TypeidData {

    private final boolean typeOperand;
    private final String operandId;
    private Type operandType;

    public TypeidData(boolean typeOperand, String operandId) {
        this.typeOperand = typeOperand;
        this.operandId = operandId;
        this.operandType = null;
    }

    public String getOperandId() {
        return operandId;
    }

    public boolean isTypeOperand() {
        return typeOperand;
    }

    public Type getOperandType() {
        Preconditions.checkArgument(typeOperand, "This typeid data does not refer to a type operand");
        return operandType;
    }

    public void setOperandType(Type operandType) {
        Preconditions.checkArgument(typeOperand, "This typeid data does not refer to a type operand");

        this.operandType = operandType;
    }

    @Override
    public String toString() {
        return "is type operand: " + isTypeOperand() + ", operand id: " + operandId;
    }
}
