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
import java.util.Collections;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.utils.NullNode;

/**
 * Represents an 'empty' node type, to be used, for instance, in cases where type is optional, or when type could not be
 * detected.
 * 
 * E.g., SimpleTypeWithKeyword expects a type after the keyword, except if it is an anonymous struct/union.
 * 
 * @author JoaoBispo
 *
 */
public class NullType extends Type implements NullNode {

    public NullType(ClavaNodeInfo info) {
        this(new LegacyToDataStore()
                .setType(new TypeData("<null type>"))
                .setNodeInfo(info)
                .getData(),
                Collections.emptyList());
        // super(new TypeData("<null type>"), info, Collections.emptyList());
    }

    public NullType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new NullType(getInfo());
    // }

    @Override
    public String getCode(String name) {
        if (name != null) {
            return getBareType() + " " + name;
        }

        return getBareType();
    }

}
