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

package pt.up.fe.specs.clang;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.language.Standard;

public interface ClangAstKeys {

    DataKey<String> CLANGAST_VERSION = KeyFactory.string("clangast_version", "");

    /**
     * If true, tries to use the platform system includes (if available), instead of the built-in system includes.
     */
    DataKey<Boolean> USE_PLATFORM_INCLUDES = KeyFactory.bool("platformIncludes")
            .setLabel("Uses the platform system includes headers (if available)");

    /**
     * Transform flags to the ClangAstDumper into a DataStore.
     * 
     * @param flags
     * @return
     */
    static DataStore toDataStore(List<String> flags) {
        DataStore config = DataStore.newInstance(ClavaOptions.STORE_DEFINITION, false);
        final String stdPrefix = "-std=";
        final String clangAstDumperPrefix = "-clang-dumper=";

        // Search options
        List<String> parsedFlags = new ArrayList<>();
        for (int i = 0; i < flags.size(); i++) {
            String flag = flags.get(i);

            // If standard flag, add option
            if (flag.startsWith(stdPrefix)) {
                Standard standard = Standard.getEnumHelper().getValuesTranslationMap()
                        .get(flag.substring(stdPrefix.length()));

                if (config.hasValue(ClavaOptions.STANDARD)) {
                    ClavaLog.info("Overriding previous standard " + config.get(ClavaOptions.STANDARD) + " with "
                            + standard);
                }

                config.set(ClavaOptions.STANDARD, standard);

                continue;
            }

            // If ClangAstDumper version, parse option
            if (flag.startsWith(clangAstDumperPrefix)) {
                String version = flag.substring(clangAstDumperPrefix.length());
                config.set(ClangAstKeys.CLANGAST_VERSION, version);
                continue;
            }

            parsedFlags.add(flag);
        }

        config.add(ClavaOptions.FLAGS, parsedFlags.stream().collect(Collectors.joining(" ")));

        return config;
    }

}
