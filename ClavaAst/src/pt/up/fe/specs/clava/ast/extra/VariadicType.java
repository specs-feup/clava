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

package pt.up.fe.specs.clava.ast.extra;

import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.data.TypeData;

/**
 * Represents an optional node in the tree.
 * 
 * <p>
 * This class exists for compatibility purposes with Clang AST, when parsing.
 * 
 * @author JoaoBispo
 *
 */
public class VariadicType extends Type {

    public VariadicType(ClavaNodeInfo info) {
        super(new TypeData("VariadicType"), info, Collections.emptyList());
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new VariadicType(getInfo());
    }

    @Override
    public String getCode(String name) {
        if (name != null) {
            throw new RuntimeException("Not implemented when name is not null");
        }

        return "...";
    }

}
