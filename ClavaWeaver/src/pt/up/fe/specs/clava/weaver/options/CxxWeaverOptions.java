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

package pt.up.fe.specs.clava.weaver.options;

import java.util.HashMap;
import java.util.Map;

import org.lara.interpreter.weaver.options.OptionArguments;
import org.lara.interpreter.weaver.options.WeaverOption;
import org.lara.interpreter.weaver.options.WeaverOptionBuilder;
import org.suikasoft.jOptions.Datakey.DataKey;

import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.weaver.CxxWeaver;

public class CxxWeaverOptions {

    private static final Map<String, WeaverOption> WEAVER_OPTIONS;
    static {
        WEAVER_OPTIONS = new HashMap<>();
        WEAVER_OPTIONS.put(ClavaOptions.STANDARD.getName(),
                WeaverOptionBuilder.build("std", "standard", OptionArguments.ONE_ARG, "C/C++ standard",
                        "What C/C++ standard should be used. Currently supported standards: "
                                + Standard.getEnumHelper().getAvailableOptions(),
                        ClavaOptions.STANDARD));

        addOneArgOption(ClavaOptions.FLAGS, "fs", "flags", "flags string",
                "String with C/C++ compiler flags");

        WEAVER_OPTIONS.put(CxxWeaverOption.DISABLE_WEAVING.getName(), WeaverOptionBuilder.build("nw", "no-weaving",
                "Disables weaving of source code, only runs the LARA aspect", CxxWeaverOption.DISABLE_WEAVING));

        addBooleanOption(CxxWeaverOption.CHECK_SYNTAX, "cs", "check-syntax", "Checks syntax of woven code");

        addBooleanOption(CxxWeaverOption.CLEAN_INTERMEDIATE_FILES, "cl", "clean", "Clean intermediate files");

        addBooleanOption(CxxWeaverOption.DISABLE_CODE_GENERATION, "ncg", "no-code-gen",
                "Disables automatic code generation");

        addBooleanOption(CxxWeaverOption.GENERATE_MODIFIED_CODE_ONLY, "gom", "generate-only-if-modified",
                "Generate code from AST for a file only if file is modified by a LARA action. Otherwise, the original files is copied");

        WEAVER_OPTIONS.put(CxxWeaverOption.DISABLE_CLAVA_INFO.getName(),
                WeaverOptionBuilder.build("nci", "no-clava-info",
                        "Disables printing of information about Clava", CxxWeaverOption.DISABLE_CLAVA_INFO));

        addOneArgOption(CxxWeaverOption.HEADER_INCLUDES, "ih", "includes-headers",
                "dir1[,dir2]*",
                "Include folders for C/C++ headers. Include files that are used in C/C++ files are processed by Clava and appear in the AST.");

        addOneArgOption(CxxWeaverOption.SYSTEM_INCLUDES, "is", "includes-system",
                "dir1[,dir2]*",
                "Include folders for C/C++ headers that should be considered 'system libraries'. System libraries are not processed by Clava and do not appear in the AST.");

        addOneArgOption(CxxWeaverOption.WOVEN_CODE_FOLDERNAME, "of", "output-foldername",
                "dir",
                "Sets the name of the woven code folder (default value: '" + CxxWeaver.getWovenCodeFoldername()
                        + "')");

        addBooleanOption(CxxWeaverOption.FLATTEN_WOVEN_CODE_FOLDER_STRUCTURE, "ff", "flatten-woven-folders",
                "Flattens woven code folder structure. Otherwise, attempts to maintain original structure specified in input sources.");

        addBooleanOption(ClavaOptions.CUSTOM_RESOURCES, "cr", "custom-resources",
                "Enables custom resource files (e.g., clang_ast.resources)");
    }

    private static final void addBooleanOption(DataKey<?> key, String shortOption, String longOption,
            String description) {
        WEAVER_OPTIONS.put(key.getName(), WeaverOptionBuilder.build(shortOption, longOption, description, key));
    }

    private static final void addOneArgOption(DataKey<?> key, String shortOption, String longOption, String argName,
            String description) {

        WEAVER_OPTIONS.put(key.getName(),
                WeaverOptionBuilder.build(shortOption, longOption, OptionArguments.ONE_ARG, argName, description, key));

    }

    public static WeaverOption getOption(DataKey<?> key) {
        WeaverOption option = WEAVER_OPTIONS.get(key.getName());
        if (option != null) {
            return option;
        }

        return WeaverOptionBuilder.build(key);
    }

}
