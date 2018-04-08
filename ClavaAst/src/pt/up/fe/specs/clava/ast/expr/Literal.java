/**
 * Copyright 2017 SPeCS.
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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.data2.ExprDataV2;
import pt.up.fe.specs.clava.ast.expr.data2.LiteralData;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

/**
 * Represents a literal node.
 * 
 * @author JoaoBispo
 *
 */
public abstract class Literal extends Expr {

    public Literal(ExprDataV2 data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /**
     * 
     * @param exprData
     * @param info
     * @param children
     */
    public Literal(ExprData exprData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(exprData, info, children);
    }

    @Override
    public LiteralData getData() {
        return (LiteralData) super.getData();
    }

    // public abstract String getLiteral();
    public String getLiteral() {
        if (getData() != null) {
            return getData().getSourceLiteral();
        }

        throw new NotImplementedException(this);
    }

    @Override
    public String getCode() {
        return getLiteral();
    }
}
