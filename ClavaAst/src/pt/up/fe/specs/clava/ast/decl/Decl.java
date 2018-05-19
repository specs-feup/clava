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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;

/**
 * Represents one declaration (or definition).
 * 
 * @author JoaoBispo
 *
 */
public abstract class Decl extends ClavaNode {

    /// DATAKEYS BEGIN

    public final static DataKey<Boolean> IS_IMPLICIT = KeyFactory.bool("isImplicit");

    public final static DataKey<Boolean> IS_USED = KeyFactory.bool("isUsed");

    public final static DataKey<Boolean> IS_REFERENCED = KeyFactory.bool("isReferenced");

    public final static DataKey<Boolean> IS_INVALID_DECL = KeyFactory.bool("isInvalidDecl");

    public final static DataKey<List<Attribute>> ATTRIBUTES = KeyFactory.generic("attributes", new ArrayList<>());

    /// DATAKEYS END

    private final DeclData data;

    public Decl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

        this.data = null;
    }

    /**
     * Legacy support.
     * 
     * @param data
     * @param info
     * @param children
     */
    public Decl(DeclData data, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);

        this.data = data;
    }

    public DeclData getDeclData() {
        return data;
    }

}
