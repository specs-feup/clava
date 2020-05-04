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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Operator;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AOp;

public class CxxOp extends AOp {

    private final Operator op;

    public CxxOp(Operator op) {
        super(new CxxExpression(op));

        this.op = op;
    }

    @Override
    public String getKindImpl() {
        return op.getKindName();
    }

    @Override
    public Boolean getIsBitwiseImpl() {
        return op.isBitwise();
    }

    @Override
    public ClavaNode getNode() {
        return op;
    }

    @Override
    public String getOperatorImpl() {
        return op.getOperatorCode();
    }

}
