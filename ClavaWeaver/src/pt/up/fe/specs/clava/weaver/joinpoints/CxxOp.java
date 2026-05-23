/**
 * Copyright 2019 SPeCS.
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

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ast.expr.Operator;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AOp;
import pt.up.fe.specs.clava.weaver.enums.OpKind;

public class CxxOp<Self extends CxxOp<Self>> extends AOp<Self> {

    public CxxOp(Operator op, CxxWeaver weaver) {
        super(op, weaver);
    }

    @Override
    public Operator getNodeImpl() {
        return (Operator) super.getNodeImpl();
    }

    @Override
    public OpKind getKindImpl() {
        var op = this.getNodeImpl();

        try {
            return OpKind.fromDisplay(op.getKindName());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Could not determine operator kind for operator with code '" + op.getOperatorCode()
                    + "' and kind name '" + op.getKindName() + "'", e);
        }
    }

    @Override
    public boolean getIsBitwiseImpl() {
        return this.getNodeImpl().isBitwise();
    }

    @Override
    public String getOperatorImpl() {
        return this.getNodeImpl().getOperatorCode();
    }

}
