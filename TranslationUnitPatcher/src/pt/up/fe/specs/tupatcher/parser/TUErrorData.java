/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.tupatcher.parser;

import java.util.HashMap;
import java.util.Map;

import org.suikasoft.jOptions.DataStore.ADataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

public class TUErrorData extends ADataClass<TUErrorData> {

    /**
     * Example of an Integer field.
     */
    public final static DataKey<Integer> ERROR_NUMBER = KeyFactory.integer("errorNumber")
            .setDefault(() -> 20);

    /**
     * Example of a Map field.
     */
    public final static DataKey<Map<String, String>> MAP = KeyFactory.generic("map",
            () -> (Map<String, String>) new HashMap<String, String>())
            .setDefault(HashMap::new);
}
