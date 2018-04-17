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
import pt.up.fe.specs.clava.ast.type.Type;

public class AlignedAttrData extends AttributeData {

    private final String spelling;
    private final boolean isExpr;
    // private final String nodeId;
    private Expr expr;
    private Type type;

    // public AlignedAttrData(String spelling, boolean isExpr, String nodeId, Expr expr, Type type, AttributeData data)
    // {
    public AlignedAttrData(String spelling, boolean isExpr, Expr expr, Type type, AttributeData data) {
        super(data);

        this.spelling = spelling;
        this.isExpr = isExpr;
        // this.nodeId = nodeId;
        this.expr = expr;
        this.type = type;
    }

    public AlignedAttrData(AlignedAttrData data) {
        // this(data.spelling, data.isExpr, data.nodeId, data.expr, data.type, data);
        this(data.spelling, data.isExpr, data.expr, data.type, data);
    }

    public String getSpelling() {
        return spelling;
    }

    public boolean isExpr() {
        return isExpr;
    }

    public Expr getExpr() {
        return expr;
    }

    public Type getType() {
        return type;
    }

    /*
    @Override
    protected void postProcess(ClavaDataPostProcessing data) {
        // Call super
        super.postProcess(data);
    
        SpecsCheck.checkNotNull(nodeId, () -> "Expected 'nodeId' in node '" + getId() + "' to be non-null");
    
        if (isExpr) {
            this.expr = data.getExpr(nodeId);
        } else {
            this.type = data.getType(nodeId);
        }
    
    }
    */

    public String getCode() {
        if (isExpr) {
            return getExpr().getCode();
        } else {
            return getType().getCode();
        }
    }

}
