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

package pt.up.fe.specs.clava.ast.expr.data2;

import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.clava.ast.ClavaDataPostProcessing;
import pt.up.fe.specs.clava.ast.expr.enums.ObjectKind;
import pt.up.fe.specs.clava.ast.expr.enums.ValueKind;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsCheck;

public class ExprDataV2 extends ClavaData {

    private final String parsedTypeId;
    private final ValueKind valueKind;
    private final ObjectKind objectKind;

    private Type type;

    public static ExprDataV2 empty() {
        return new ExprDataV2(null, ValueKind.getDefault(), ObjectKind.ORDINARY, ClavaData.empty());
    }

    public ExprDataV2(String parsedTypeId, ValueKind valueKind, ObjectKind objectKind, ClavaData clavaData) {
        super(clavaData);

        this.parsedTypeId = parsedTypeId;
        this.valueKind = valueKind;
        this.objectKind = objectKind;

        this.type = null;
    }

    public ExprDataV2(ExprDataV2 data) {
        this(data.parsedTypeId, data.valueKind, data.objectKind, (ClavaData) data);

        this.type = data.type;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public ValueKind getValueKind() {
        return valueKind;
    }

    public ObjectKind getObjectKind() {
        return objectKind;
    }

    // public ExprDataV2(ExprDataV2 data) {
    // this(data);
    // }

    @Override
    public ExprDataV2 copy() {
        return new ExprDataV2(this);
    }

    @Override
    public String toString() {
        String typeId = getType() == null ? "null" : getType().getExtendedId().orElse("no_id");
        return toString(super.toString(),
                "type: " + typeId + ", valueKind: " + valueKind + ", objectKind: " + objectKind);
    }

    @Override
    protected void postProcess(ClavaDataPostProcessing data) {
        // Call super
        super.postProcess(data);

        SpecsCheck.checkNotNull(parsedTypeId, () -> "Expected 'parsedTypeId' in node '" + getId() + "' to be non-null");
        // Copy type, to be able to freely modify it
        // this.type = data.getType(parsedTypeId).copy();
        // Model without copy, where users have to be careful to not modify types but copies of types
        // Model where types are immutable, any set of a type returns a new node
        this.type = data.getType(parsedTypeId);
    }
}
