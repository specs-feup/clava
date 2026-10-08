/**
 * Copyright 2020 SPeCS.
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

import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AIntLiteral;

public class CxxIntLiteral<Self extends CxxIntLiteral<Self>> extends AIntLiteral<Self> {

    public CxxIntLiteral(IntegerLiteral literal, CxxWeaver weaver) {
        super(literal, weaver);
    }

    @Override
    public IntegerLiteral getNodeImpl() {
        return (IntegerLiteral) super.getNodeImpl();
    }

    @Override
    public long getValueImpl() {
        return this.getNodeImpl().get(IntegerLiteral.VALUE).longValue();
    }
}
