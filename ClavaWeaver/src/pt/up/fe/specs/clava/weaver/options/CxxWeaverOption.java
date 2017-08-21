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

import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.weaver.CxxWeaver;

public interface CxxWeaverOption {

    DataKey<String> WEAVED_CODE_FOLDERNAME = KeyFactory.string("Weaved code foldername")
            .setLabel("Name of weaved code folder")
            .setDefault(() -> CxxWeaver.getWeavedCodeFoldername());

    DataKey<Boolean> DISABLE_CLAVA_INFO = KeyFactory.bool("Disable Clava Info")
            .setLabel("Disable Clava execution information");

    DataKey<Boolean> CHECK_SYNTAX = KeyFactory.bool("Check C/CXX Syntax")
            .setLabel("Check C/C++ syntax (performs additional parsing step)");

    DataKey<Boolean> CLEAN_INTERMEDIATE_FILES = KeyFactory.bool("Clean intermediate files")
            .setDefault(() -> true);

    DataKey<FileList> LIBRARY_INCLUDES = LaraIKeyFactory.folderList("library includes")
            .setLabel("Library Includes")
            .setDefault(() -> FileList.newInstance());

    DataKey<Boolean> DISABLE_WEAVING = KeyFactory.bool("Disable Weaving")
            .setLabel("Disable Weaving (only executes the LARA code, does not parse C/C++ code)");

    StoreDefinition STORE_DEFINITION = new StoreDefinitionBuilder("C/C++ Weaver")
            .addKeys(ClavaOptions.STORE_DEFINITION.getKeys())
            .addKeys(WEAVED_CODE_FOLDERNAME, DISABLE_CLAVA_INFO, CHECK_SYNTAX, CLEAN_INTERMEDIATE_FILES,
                    LIBRARY_INCLUDES, DISABLE_WEAVING)
            .build();

}
