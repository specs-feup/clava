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

package pt.up.fe.specs.clava.ast.expr.data;

import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.type.Type;

public class ExprData {

    private static final ExprData EMPTY = new ExprData(ClavaNodeFactory.nullType(ClavaNodeInfo.undefinedInfo()),
            ValueKind.getDefault());

    private final Type type;
    private final ValueKind valueKind;
    private final ObjectKind objectKind;

    public ExprData(Type type) {
        this(type, ValueKind.getDefault());
    }

    public ExprData(Type type, ValueKind valueKind) {
        this(type, valueKind, ObjectKind.ORDINARY);
    }

    public ExprData(Type type, ValueKind valueKind, ObjectKind objectKind) {
        this.type = type;
        this.valueKind = valueKind;
        this.objectKind = objectKind;
    }

    public Type getType() {
        return type;
    }

    public ValueKind getValueKind() {
        return valueKind;
    }

    public ObjectKind getObjectKind() {
        return objectKind;
    }

    public static ExprData empty() {
        return EMPTY;
    }

    @Override
    public String toString() {
        return "ValueKind:" + valueKind + "; ObjectKind:" + objectKind + "; Type:" + type.getCode();
    }

    /**
     * Currently all fields of ExprData are immutable, and returns an instance of itself.
     * 
     * @return
     */
    // public ExprData copy() {
    // return this;
    // }

}
