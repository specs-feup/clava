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

import java.util.List;

import pt.up.fe.specs.clava.ast.expr.data.offsetof.OffsetOfComponent;
import pt.up.fe.specs.clava.ast.type.Type;

public class OffsetOfData {

    private final Type sourceType;
    private final List<OffsetOfComponent> components;

    public OffsetOfData(Type sourceType, List<OffsetOfComponent> components) {
        this.sourceType = sourceType;
        this.components = components;
    }

    public List<OffsetOfComponent> getComponents() {
        return components;
    }

    public Type getSourceType() {
        return sourceType;
    }

}
