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

package pt.up.fe.specs.clava.weaver.options;

import org.lara.interpreter.joptions.config.interpreter.LaraIKeyFactory;
import org.lara.interpreter.joptions.keys.FileList;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;
import org.suikasoft.jOptions.storedefinition.StoreDefinitionBuilder;

import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.weaver.CxxWeaver;

public interface CxxWeaverOption {

    DataKey<String> WOVEN_CODE_FOLDERNAME = KeyFactory.string("Weaved code foldername")
            .setLabel("Name of woven code folder")
            .setDefault(() -> CxxWeaver.getWovenCodeFoldername());

    DataKey<Boolean> DISABLE_CLAVA_INFO = KeyFactory.bool("Disable Clava Info")
            .setLabel("Disable Clava execution information");

    DataKey<Boolean> CHECK_SYNTAX = KeyFactory.bool("Check C/CXX Syntax")
            .setLabel("Check C/C++ syntax (performs additional parsing step)");

    DataKey<Boolean> CLEAN_INTERMEDIATE_FILES = KeyFactory.bool("Clean intermediate files")
            .setDefault(() -> true);

    DataKey<FileList> HEADER_INCLUDES = LaraIKeyFactory.folderList("header includes")
            .setLabel("Normal Includes")
            .setDefault(() -> FileList.newInstance());

    DataKey<FileList> SYSTEM_INCLUDES = LaraIKeyFactory.folderList("library includes")
            .setLabel("System Includes")
            .setDefault(() -> FileList.newInstance());

    DataKey<Boolean> DISABLE_WEAVING = KeyFactory.bool("Disable Weaving")
            .setLabel("Disable weaving (only executes the LARA code, does not parse C/C++ code)");

    DataKey<Boolean> DISABLE_CODE_GENERATION = KeyFactory.bool("Disable Code Generation")
            .setLabel("Disable code generation (still possible to generate code explictly from LARA)");

    DataKey<Boolean> GENERATE_MODIFIED_CODE_ONLY = KeyFactory.bool("Generate Modified Code Only")
            .setLabel("Generate code from AST only if tree is modified (otherwise copies original file)");

    DataKey<Boolean> GENERATE_CMAKE_HELPER_FILES = KeyFactory.bool("Generate CMake Integration Helper Files")
            .setLabel("Generate helper files to be used by Clava CMake integration modules");

    DataKey<Boolean> FLATTEN_WOVEN_CODE_FOLDER_STRUCTURE = KeyFactory.bool("Flatten woven code folder structure");

    DataKey<Boolean> UNIT_TESTING_MODE = KeyFactory.bool("Unit Testing Mode")
            .setLabel("Runs the Clava Unit-Tester");

    StoreDefinition STORE_DEFINITION = new StoreDefinitionBuilder("C/C++ Weaver")
            .addKeys(ClavaOptions.STORE_DEFINITION.getKeys())
            .addKeys(WOVEN_CODE_FOLDERNAME, DISABLE_CLAVA_INFO, CHECK_SYNTAX, CLEAN_INTERMEDIATE_FILES,
                    HEADER_INCLUDES, SYSTEM_INCLUDES, DISABLE_WEAVING, DISABLE_CODE_GENERATION,
                    // GENERATE_MODIFIED_CODE_ONLY, GENERATE_CMAKE_HELPER_FILES)
                    GENERATE_MODIFIED_CODE_ONLY, GENERATE_CMAKE_HELPER_FILES, FLATTEN_WOVEN_CODE_FOLDER_STRUCTURE)
            // GENERATE_MODIFIED_CODE_ONLY, FLATTEN_WOVEN_CODE_FOLDER_STRUCTURE, UNIT_TESTING_MODE)
            .addKey(ParallelCodeParser.PARALLEL_PARSING)
            .addKey(ParallelCodeParser.PARSING_NUM_THREADS)
            .addKey(ClangAstKeys.USE_PLATFORM_INCLUDES)
            .build();

}
