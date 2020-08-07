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

import org.suikasoft.jOptions.streamparser.LineStreamWorker;

import pt.up.fe.specs.util.utilities.LineStream;

public class ExampleWorkers {

    public static LineStreamWorker<TUErrorData> newStoreKeyValueWorker() {
        return LineStreamWorker.newInstance("<STORE_KEY_VALUE>",
                data -> data.set(TUErrorData.MAP_EXAMPLE, new HashMap<>()), ExampleWorkers::storeKeyValue);
    }

    private static void storeKeyValue(LineStream lines, TUErrorData data) {
        // Expects two lines, one with the key, another with the value
        String key = lines.nextLine();
        String value = lines.nextLine();

        data.get(TUErrorData.MAP_EXAMPLE).put(key, value);
    }
}
