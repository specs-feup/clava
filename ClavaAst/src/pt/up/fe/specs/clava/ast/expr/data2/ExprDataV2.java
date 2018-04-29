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

import java.util.Optional;

import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.clava.ast.expr.ImplicitCastExpr;
import pt.up.fe.specs.clava.ast.expr.enums.ObjectKind;
import pt.up.fe.specs.clava.ast.expr.enums.ValueKind;
import pt.up.fe.specs.clava.ast.type.Type;

/**
 * @deprecated
 * @author JoaoBispo
 *
 */
@Deprecated
public class ExprDataV2 extends ClavaData {

    // private final String typeId;
    private Type type;
    private final ValueKind valueKind;
    private final ObjectKind objectKind;

    // Optional
    private ImplicitCastExpr implicitCast;

    /*
    public static ExprDataV2 empty(ClavaData data) {
        if (data instanceof ExprDataV2) {
            return (ExprDataV2) data;
        }
    
        return new ExprDataV2(null, ValueKind.getDefault(), ObjectKind.ORDINARY, ClavaData.empty());
    }
    */
    public ExprDataV2(Type type, ValueKind valueKind, ObjectKind objectKind, ClavaData clavaData) {
        this(type, valueKind, objectKind, null, clavaData);
    }

    private ExprDataV2(Type type, ValueKind valueKind, ObjectKind objectKind, ImplicitCastExpr implicitCast,
            ClavaData clavaData) {

        super(clavaData);

        this.type = type;

        this.valueKind = valueKind;
        this.objectKind = objectKind;

        // Optional
        this.implicitCast = implicitCast;
    }

    public ExprDataV2(ExprDataV2 data) {
        // Can share type nodes
        this(data.type, data.valueKind, data.objectKind, data.implicitCast, (ClavaData) data);
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

    public Optional<ImplicitCastExpr> getImplicitCast() {
        return Optional.ofNullable(implicitCast);
    }

    public void setImplicitCast(ImplicitCastExpr implicitCast) {
        this.implicitCast = implicitCast;
    }

    // public ExprDataV2(ExprDataV2 data) {
    // this(data);
    // }

    // @Override
    // public ExprDataV2 copy() {
    // return new ExprDataV2(this);
    // }

    @Override
    public String toString() {
        String typeId = getType() == null ? "null" : getType().getExtendedId().orElse("no_id");
        // String implicitCastId = implicitCast == null ? "null" : implicitCast.getExtendedId().orElse("no_id");
        String implicitCastId = getImplicitCast().map(cast -> cast.getExtendedId().orElse("no_id")).orElse("null");
        return toString(super.toString(),
                "type: " + typeId + ", valueKind: " + valueKind + ", objectKind: " + objectKind + ", implicitCast: "
                        + implicitCastId);
    }

    /*
    @Override
    protected void postProcess(ClavaDataPostProcessing data) {
        // Call super
        super.postProcess(data);
    
        SpecsCheck.checkNotNull(typeId, () -> "Expected 'parsedTypeId' in node '" + getId() + "' to be non-null");
        // Copy type, to be able to freely modify it
        // this.type = data.getType(parsedTypeId).copy();
        // Model without copy, where users have to be careful to not modify types but copies of types
        // Model where types are immutable, any set of a type returns a new node
    
        this.type = data.getType(typeId);
    }
    */
}
