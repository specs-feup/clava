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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.Collections;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.utils.NullNode;

/**
 * Represents a non-existent declaration, where could have been one.
 * 
 * @author JoaoBispo
 *
 */
public class NullDecl extends Decl implements NullNode {

    public NullDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public static NullDecl create(ClavaNode node) {
        return create(node.getInfo());
    }

    public static NullDecl create(ClavaNodeInfo info) {
        return new NullDecl(info);
    }

    public NullDecl(ClavaNodeInfo info) {
        super(DeclData.empty(), info, Collections.emptyList());
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new NullDecl(getInfo());
    }

}
