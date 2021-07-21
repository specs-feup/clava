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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.utilities.StringList;

public interface ClangAstKeys {

    DataKey<String> CLANGAST_VERSION = KeyFactory.string("clangast_version", "");
    // DataKey<String> LIBC_VERSION = KeyFactory.string("libc_version", "");

    /**
     * If true, tries to use the platform system includes (if available), instead of the built-in system includes.
     */
    DataKey<Boolean> USE_PLATFORM_INCLUDES = KeyFactory.bool("platformIncludes")
            // .setLabel("Uses the platform system includes headers (if available)");
            .setLabel("Disable built-in lib C/C++ includes");

    DataKey<Boolean> USES_CILK = KeyFactory.bool("usesCilk");

    DataKey<StringList> IGNORE_HEADER_INCLUDES = KeyFactory.stringList("ignoreHeaderIncludes")
            .setLabel("Headers to ignore when recreating #include directives (Java regexes)");

    public static String getFlagIgnoreIncludes() {
        return "ihi";
    }

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
        final String cilkFlag = "-fcilkplus";

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

            // If Cilk flag, add option
            if (flag.equals(cilkFlag)) {
                config.set(ClangAstKeys.USES_CILK);
                continue;
            }

            if (flag.equals(getFlagIgnoreIncludes())) {
                // Must have another argument
                SpecsCheck.checkSize(flags, i + 2);

                String value = flags.get(i + 1);
                config.set(ClangAstKeys.IGNORE_HEADER_INCLUDES, StringList.getCodec().decode(value));

                // Advance an extra flag
                i++;
                continue;
            }

            parsedFlags.add(flag);
        }

        // config.add(ClavaOptions.FLAGS, parsedFlags.stream().collect(Collectors.joining(" ")));
        config.add(ClavaOptions.FLAGS_LIST, parsedFlags);

        return config;
    }

}
