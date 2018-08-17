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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * Represents the declaration of a friend entity.
 * 
 * <p>
 * Friend node can either be a Type or a Decl.
 * 
 * @author JoaoBispo
 *
 */
public class FriendDecl extends Decl {

    public FriendDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public FriendDecl(DeclData declData, ClavaNodeInfo info, ClavaNode friendNode) {
    // this(declData, info, Arrays.asList(friendNode));
    // }
    //
    // private FriendDecl(DeclData declData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    // super(declData, info, children);
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new FriendDecl(getDeclData(), getInfo(), Collections.emptyList());
    // }

    public ClavaNode getFriendNode() {
        return getChild(0);
    }

    @Override
    public String getCode() {

        String friendCode = getFriendNode().getCode();

        // Check if it has new lines at the beginning
        if (friendCode.startsWith(ln())) {
            friendCode = friendCode.substring(ln().length());
        }

        return "friend " + friendCode;
    }

}
