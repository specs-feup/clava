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

package pt.up.fe.specs.tupatcher;

import java.io.File;
import java.util.Arrays;

import org.suikasoft.jOptions.DataStore.ADataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;

import pt.up.fe.specs.util.utilities.StringList;

public class TUPatcherConfig extends ADataClass<TUPatcherConfig> {

    public static final DataKey<StringList> SOURCE_PATHS = KeyFactory.stringList("sourcePaths");
    public static final DataKey<File> OUTPUT_FOLDER = KeyFactory.folder("outputFolder");
    public static final DataKey<Integer> MAX_FILES = KeyFactory.integer("maxFiles", 600);
    public static final DataKey<Integer> MAX_ITERATIONS = KeyFactory.integer("maxIterations", 100);
    public static final DataKey<Boolean> PARALLEL = KeyFactory.bool("parallel").setDefault(() -> true);
    public static final DataKey<Integer> NUM_THREADS = KeyFactory.integer("numThreads");
    public static final DataKey<StringList> SOURCE_EXTENSIONS = KeyFactory.stringList("sourceExtensions",
            Arrays.asList("c", "cpp"));

    public TUPatcherConfig() {

    }

    public TUPatcherConfig(DataStore dataStore) {
        super(dataStore);
    }

    public static StoreDefinition getDefinition() {
        return new TUPatcherConfig().getStoreDefinitionTry().get();
    }

}
