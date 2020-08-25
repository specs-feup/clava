/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.clava.weaver;

import org.suikasoft.jOptions.DataStore.DataClass;
import org.suikasoft.jOptions.DataStore.DataClassWrapper;

import pt.up.fe.specs.clava.ClavaNode;

public class CxxWeaverDataClass extends DataClassWrapper<CxxWeaverDataClass> {

    public CxxWeaverDataClass(DataClass<?> data) {
        super(data);
    }

    @Override
    protected CxxWeaverDataClass getThis() {
        return this;
    }

    @Override
    public Object getValue(String key) {
        var value = super.getValue(key);

        // Special case

        if (value instanceof ClavaNode) {
            return CxxJoinpoints.create((ClavaNode) value);
        }

        return value;
    }

}
