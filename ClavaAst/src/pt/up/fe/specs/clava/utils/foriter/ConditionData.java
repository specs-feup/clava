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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.utils.foriter;

import pt.up.fe.specs.clava.ast.expr.Expr;

public class ConditionData {

    private final Expr value;
    private final ForCondRelation relation;

    public ConditionData(Expr value, ForCondRelation relation) {
        this.value = value;
        this.relation = relation;
    }

    public Expr getValue() {
        return value;
    }

    /**
     * The relation between the iteration variable and the condition value.
     * 
     * <p>
     * The relation is always expressed as if the iteration variable is on the left-hand of the condition, and the
     * condition value on the right-hand of the condition.
     * 
     * @return
     */
    public ForCondRelation getRelation() {
        return relation;
    }

    @Override
    public String toString() {
        return "ConditionData [relation=" + relation + ", value=" + value + "]";
    }

}
