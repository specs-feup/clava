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

package pt.up.fe.specs.clava.ast.attr.data;

import pt.up.fe.specs.clava.ast.ClavaDataPostProcessing;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.util.SpecsCheck;

public class AlignedExprAttrData extends AlignedAttrData {

    private final String exprId;
    private Expr expr;

    public AlignedExprAttrData(String exprId, Expr expr, AlignedAttrData data) {
        super(data);

        this.exprId = exprId;
        this.expr = expr;
    }

    public AlignedExprAttrData(AlignedExprAttrData data) {
        this(data.exprId, data.expr, data);
    }

    public Expr getExpr() {
        return expr;
    }

    @Override
    protected void postProcess(ClavaDataPostProcessing data) {
        // Call super
        super.postProcess(data);

        SpecsCheck.checkNotNull(exprId, () -> "Expected 'exprId' in node '" + getId() + "' to be non-null");

        this.expr = data.getExpr(exprId);
    }

}
