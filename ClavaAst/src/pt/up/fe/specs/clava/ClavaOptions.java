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

package pt.up.fe.specs.clava;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;
import org.suikasoft.jOptions.storedefinition.StoreDefinitionBuilder;
import org.suikasoft.jOptions.storedefinition.StoreDefinitionProvider;

import pt.up.fe.specs.clava.language.Standard;

public interface ClavaOptions extends StoreDefinitionProvider {

    DataKey<Standard> STANDARD = KeyFactory.enumeration("C/C++ Standard", Standard.class)
            .setDefault(() -> Standard.CXX11);

    DataKey<String> FLAGS = KeyFactory.string("Compiler Flags", "");

    DataKey<Boolean> CUSTOM_RESOURCES = KeyFactory.bool("Clava Custom Resources")
            .setLabel("Enable custom resource files");

    StoreDefinition STORE_DEFINITION = new StoreDefinitionBuilder("Clava")
            .addKeys(STANDARD, FLAGS, CUSTOM_RESOURCES)
            .build();

    @Override
    default StoreDefinition getStoreDefinition() {
        return STORE_DEFINITION;
    }

    static DataStore toDataStore(List<String> flags) {
        DataStore config = DataStore.newInstance(ClavaOptions.STORE_DEFINITION);
        final String stdPrefix = "-std=";

        // Search options
        List<String> parsedFlags = new ArrayList<>();
        for (int i = 0; i < flags.size(); i++) {
            String flag = flags.get(i);

            // If standard flag, add option
            if (flag.startsWith(stdPrefix)) {
                Standard standard = Standard.getEnumHelper().getTranslationMap()
                        .get(flag.substring(stdPrefix.length()));
                // config.add(ClavaOptions.STANDARD, standard);
                Optional<Standard> previousStd = config.set(ClavaOptions.STANDARD, standard);
                previousStd
                        .ifPresent(std -> ClavaLog.info("Overriding previous standard " + std + " with " + standard));

                continue;
            }

            parsedFlags.add(flag);
        }

        config.add(ClavaOptions.FLAGS, parsedFlags.stream().collect(Collectors.joining(" ")));

        return config;
    }

}
