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

public class ConditionalOperator extends AbstractConditionalOperator {

    public ConditionalOperator(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Expr getCondition() {
        return getChild(Expr.class, 0);
    }

    public Expr getTrueExpr() {
        return getChild(Expr.class, 1);
    }

    public Expr getFalseExpr() {
        return getChild(Expr.class, 2);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append(getCondition().getCode())
                .append(" ? ")
                .append(getTrueExpr().getCode())
                .append(" : ")
                .append(getFalseExpr().getCode());

        return code.toString();
    }

    @Override
    public boolean isBitwise() {
        return false;
    }

    @Override
    public String getKindName() {
        return "ternary";
    }

    @Override
    public String getOperatorCode() {
        return "?";
    }
}
