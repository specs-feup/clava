/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * Sugar for parentheses used when specifying types.
 * 
 * @author JBispo
 *
 */
public class ParenType extends Type {

    public ParenType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Type getInnerType() {
        return get(UNQUALIFIED_DESUGARED_TYPE).get();
    }

    public void setInnerType(Type type) {
        set(UNQUALIFIED_DESUGARED_TYPE, Optional.of(type));
    }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {

        if (name == null) {
            return "(" + getInnerType().getCode(sourceNode) + ")";
        }

        return getInnerType().getCode(sourceNode, "(" + name + ")");
    }

}
