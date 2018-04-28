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

import pt.up.fe.specs.clava.ast.expr.Expr;

public class AlignedExprAttrData extends AttributeData {

    private String spelling;
    private Expr expr;

    /**
     * Full constructor.
     * 
     * @param spelling
     * @param expr
     * @param data
     */
    public AlignedExprAttrData(String spelling, Expr expr, AttributeData data) {
        super(data);

        this.spelling = spelling;
        this.expr = expr;
    }

    /**
     * Copy constructor.
     * 
     * @param data
     */
    public AlignedExprAttrData(AlignedExprAttrData data) {
        super(data);
        setData(data);
    }

    /**
     * Empty constructor.
     */
    public AlignedExprAttrData() {
        this(null, null, new AttributeData());
    }

    public AlignedExprAttrData setData(AlignedExprAttrData data) {
        super.setData(data);

        this.expr = data.expr;

        return this;
    }

    public String getSpelling() {
        return spelling;
    }

    public Expr getExpr() {
        return expr;
    }

}
