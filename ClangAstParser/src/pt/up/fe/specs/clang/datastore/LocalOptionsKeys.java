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

package pt.up.fe.specs.clang.datastore;

import java.util.Collections;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;
import org.suikasoft.jOptions.storedefinition.StoreDefinitionProvider;

import pt.up.fe.specs.util.utilities.StringList;

/**
 * DataKeys for local options of ClangAst.
 * 
 * @author JoaoBispo
 *
 */
public interface LocalOptionsKeys extends StoreDefinitionProvider {

    DataKey<StringList> SYSTEM_INCLUDES = KeyFactory.stringList("string_includes", Collections.emptyList());

    @Override
    default StoreDefinition getStoreDefinition() {
	return StoreDefinition.newInstance("ClangAst Local Options", SYSTEM_INCLUDES);
    }

    static LocalOptionsKeys getProvider() {
	return new LocalOptionsKeys() {
	};
    }
}
