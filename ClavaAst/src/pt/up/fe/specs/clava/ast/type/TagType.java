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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.TagDecl;
import pt.up.fe.specs.clava.language.TagKind;

public abstract class TagType extends Type {

    /// DATAKEYS BEGIN

    public final static DataKey<TagDecl> DECL = KeyFactory.object("decl", TagDecl.class);

    /// DATAKEYS END

    public TagType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public TagDecl getDecl() {
        return get(DECL);
    }

    public TagKind getTagKind() {
        return getDecl().get(TagDecl.TAG_KIND);
    }

}
