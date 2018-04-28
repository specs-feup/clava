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

public abstract class AlignedAttrData extends AttributeData {

    private String spelling;

    /**
     * Full constructor.
     * 
     * @param spelling
     * @param data
     */
    public AlignedAttrData(String spelling, AttributeData data) {
        super(data);

        this.spelling = spelling;
    }

    /**
     * Copy constructor.
     * 
     * @param data
     */
    public AlignedAttrData(AlignedAttrData data) {
        super(data);

        setData(data);
        // this(data.spelling, data);
    }

    /**
     * Empty constructor.
     */
    public AlignedAttrData() {
        this(null, new AttributeData());
    }

    public AlignedAttrData setData(AlignedAttrData data) {
        this.spelling = data.spelling;

        return this;
    }

    public String getSpelling() {
        return spelling;
    }

    public abstract boolean isExpr();

    public abstract Expr getExpr();

    public abstract Type getType();

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
        if (isExpr()) {
            return getExpr().getCode();
        } else {
            return getType().getCode();
        }
    }

}
