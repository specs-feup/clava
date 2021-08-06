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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.Decl;

public class TemplateTypeParmType extends Type {

    /// DATAKEYS BEGIN

    public final static DataKey<Integer> DEPTH = KeyFactory.integer("depth");
    public final static DataKey<Integer> INDEX = KeyFactory.integer("index");
    public final static DataKey<Boolean> IS_PACKED = KeyFactory.bool("isPacked");
    public final static DataKey<Optional<Decl>> DECL = KeyFactory.optional("decl");

    /// DATAKEYS END

    public TemplateTypeParmType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

}
