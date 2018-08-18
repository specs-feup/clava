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

import org.suikasoft.jOptions.DataStore.ADataClass;

import pt.up.fe.specs.util.exceptions.NotImplementedException;

/**
 * Represents the data of an OffsetOfExpr.
 * 
 * @author JoaoBispo
 *
 */
public abstract class OffsetOfComponent extends ADataClass<OffsetOfComponent> {

    public abstract String getCode();

    public abstract OffsetOfComponentKind getKind();

    public boolean isField() {
        return false;
    }

    public static OffsetOfComponent newInstance(OffsetOfComponentKind kind) {
        switch (kind) {
        case ARRAY:
            return new OffsetOfArray();
        case FIELD:
            return new OffsetOfField();
        default:
            throw new NotImplementedException(kind);
        }

    }
}
