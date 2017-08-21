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

package pt.up.fe.specs.clang.streamparser.data;

import com.google.common.base.Preconditions;

public class OffsetOfClangComponent {

    private final OffsetOfComponentKind kind;
    private final int expressionIndex;
    private final String fieldName;

    private OffsetOfClangComponent(OffsetOfComponentKind kind, int expressionIndex, String fieldName) {
        this.kind = kind;
        this.expressionIndex = expressionIndex;
        this.fieldName = fieldName;
    }

    public static OffsetOfClangComponent newArrayComponent(int expressionIndex) {
        Preconditions.checkArgument(expressionIndex >= 0,
                "Expression index must be greater or equal to 0, is " + expressionIndex);

        return new OffsetOfClangComponent(OffsetOfComponentKind.ARRAY, expressionIndex, null);

    }

    public static OffsetOfClangComponent newFieldComponent(String fieldName) {
        Preconditions.checkNotNull(fieldName, "fieldName must not be null");

        return new OffsetOfClangComponent(OffsetOfComponentKind.FIELD, 1, fieldName);
    }

    @Override
    public String toString() {
        return "kind:" + kind + "; exprIndex:" + expressionIndex + "; fieldName:" + fieldName;
    }

    public OffsetOfComponentKind getKind() {
        return kind;
    }

    public int getExpressionIndex() {
        return expressionIndex;
    }

    public String getFieldName() {
        return fieldName;
    }

}
