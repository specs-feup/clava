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
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsCheck;

public class AlignedTypeAttrData extends AlignedAttrData {

    private final String typeId;
    private Type type;

    public AlignedTypeAttrData(String typeId, Type type, AlignedAttrData data) {
        super(data);

        this.typeId = typeId;
        this.type = type;
    }

    public AlignedTypeAttrData(AlignedTypeAttrData data) {
        this(data.typeId, data.type, data);
    }

    public Type getType() {
        return type;
    }

    @Override
    protected void postProcess(ClavaDataPostProcessing data) {
        // Call super
        super.postProcess(data);

        SpecsCheck.checkNotNull(typeId, () -> "Expected 'exprId' in node '" + getId() + "' to be non-null");

        this.type = data.getType(typeId);
    }

}
