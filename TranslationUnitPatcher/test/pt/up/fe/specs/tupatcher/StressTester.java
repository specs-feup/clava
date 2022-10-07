/**
 * Copyright 2022 SPeCS.
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

import org.junit.Test;

import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.utilities.StringList;

public class StressTester {

    private static TUPatcherConfig folderConfig(File srcFolder) {
        var tempFolder = SpecsIo
                .getTempFolder("TranslationUnitPatcherTest_StressTester");

        SpecsIo.deleteFolderContents(tempFolder, true);
        SpecsIo.deleteOnExit(tempFolder);

        // System.out.println("TEMP: " + tempFolder.getAbsolutePath());
        // var tempSrcs = SpecsIo.mkdir(tempFolder, "src");
        var tempOutput = SpecsIo.mkdir(tempFolder, "output");
        // var castFile = testResource.write(tempSrcs);
        var config = new TUPatcherConfig();
        // config.set(TUPatcherConfig.SOURCE_PATHS, StringList.newInstance(castFile.getAbsolutePath()));
        config.set(TUPatcherConfig.SOURCE_PATHS, StringList.newInstance(srcFolder.getAbsolutePath()));
        config.set(TUPatcherConfig.OUTPUT_FOLDER, tempOutput);
        config.set(TUPatcherConfig.PARALLEL);
        config.set(TUPatcherConfig.NUM_THREADS, 8);

        return config;
    }

    @Test
    public void test1000() {
        SpecsSystem.programStandardInit();

        var testContents = SpecsIo.getResource("pt/up/fe/specs/tupatcher/almostOk.cpp");

        int numberOfSrcFiles = 40;

        var tempFolder = SpecsIo.getTempFolder("tupatcher_stress_test_" + numberOfSrcFiles);
        SpecsIo.deleteFolderContents(tempFolder);

        for (int i = 0; i < numberOfSrcFiles; i++) {
            SpecsIo.write(new File(tempFolder, "stress_test_" + i + ".cpp"), testContents);
        }

        var folderConfig = folderConfig(tempFolder);

        new TUPatcherLauncher(folderConfig).execute();

        // System.out.println("TEST CONTENTS: " + testContents);

    }

}
