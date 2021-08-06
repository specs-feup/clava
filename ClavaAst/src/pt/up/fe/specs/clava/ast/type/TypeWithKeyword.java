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
import pt.up.fe.specs.clava.ast.type.enums.ElaboratedTypeKeyword;

/**
 * Represents Type nodes that have an ElaboratedTypeKeywordOld.
 * 
 * @author JoaoBispo
 *
 */
public abstract class TypeWithKeyword extends Type {

    /// DATAKEYS BEGIN

    public final static DataKey<ElaboratedTypeKeyword> ELABORATED_TYPE_KEYWORD = KeyFactory
            .enumeration("elaboratedTypeKeyword", ElaboratedTypeKeyword.class);

    /// DATAKEYS END

    public TypeWithKeyword(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public ElaboratedTypeKeyword getKeyword() {
        return get(ELABORATED_TYPE_KEYWORD);
    }

}
