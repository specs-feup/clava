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

package pt.up.fe.specs.clava.ast.expr.data.offsetof;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

public class OffsetOfIdentifier extends OffsetOfComponent {

    /// DATAKEYS BEGIN

    public final static DataKey<String> FIELD_NAME = KeyFactory.string("fieldName");

    /// DATAKEYS END

    // private final String fieldName;
    //
    // public OffsetOfField(String fieldName) {
    // this.fieldName = fieldName;
    // }

    public String getFieldName() {
        return get(FIELD_NAME);
    }

    @Override
    public boolean isField() {
        return true;
    }

    @Override
    public String getCode() {
        return getFieldName();
    }

    @Override
    public OffsetOfComponentKind getKind() {
        return OffsetOfComponentKind.IDENTIFIER;
    }
}
