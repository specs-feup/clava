/**
 * Copyright 2021 SPeCS.
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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.providers.ResourceProvider;
import pt.up.fe.specs.util.utilities.StringList;

public class PatcherTest {

    @BeforeClass
    public static void init() {
        SpecsSystem.programStandardInit();
    }

    private static TUPatcherConfig newSingleFileConfig(ResourceProvider testResource) {
        var tempFolder = SpecsIo
                .getTempFolder("TranslationUnitPatcherTest_" + SpecsIo.removeExtension(testResource.getResourceName()));

        SpecsIo.deleteFolderContents(tempFolder, true);

        System.out.println("TEMP: " + tempFolder.getAbsolutePath());
        var tempSrcs = SpecsIo.mkdir(tempFolder, "src");
        var tempOutput = SpecsIo.mkdir(tempFolder, "output");
        var castFile = testResource.write(tempSrcs);
        var config = new TUPatcherConfig();
        config.set(TUPatcherConfig.SOURCE_PATHS, StringList.newInstance(castFile.getAbsolutePath()));
        config.set(TUPatcherConfig.OUTPUT_FOLDER, tempOutput);
        return config;
    }

    // @Test
    // public void testCast() {
    // var config = newSingleFileConfig(PatcherTestResource.CAST);
    // assertEquals(0, new TUPatcherLauncher(config).execute());
    // }

    @Test
    public void testClasses() {
        var config = newSingleFileConfig(PatcherTestResource.CLASSES);
        assertEquals(0, new TUPatcherLauncher(config).execute());
    }

    @Test
    public void testNested() {
        var config = newSingleFileConfig(PatcherTestResource.NESTED);
        assertEquals(0, new TUPatcherLauncher(config).execute());
    }

    // @Test
    // public void testOperators() {
    // var config = newSingleFileConfig(PatcherTestResource.OPERATORS);
    // assertEquals(0, new TUPatcherLauncher(config).execute());
    // }

    @Test
    public void testPointers() {
        var config = newSingleFileConfig(PatcherTestResource.POINTERS);
        assertEquals(0, new TUPatcherLauncher(config).execute());
    }

    @Test
    public void testStructs() {
        var config = newSingleFileConfig(PatcherTestResource.STRUCTS);
        assertEquals(0, new TUPatcherLauncher(config).execute());
    }

    @Test
    public void testParallelExecution() {
        var baseTests = Arrays.asList(PatcherTestResource.NESTED, PatcherTestResource.CLASSES,
                PatcherTestResource.POINTERS, PatcherTestResource.STRUCTS);

        var tempFolder = SpecsIo.getTempFolder("TranslationUnitPatcherTest_parallel");

        SpecsIo.deleteFolderContents(tempFolder, true);

        var tempSrcs = SpecsIo.mkdir(tempFolder, "src");
        var tempOutput = SpecsIo.mkdir(tempFolder, "output");

        // Write resource to the folder
        int repetitions = 3;
        for (int i = 0; i < repetitions; i++) {
            for (var baseTest : baseTests) {
                var newFilename = SpecsIo.removeExtension(baseTest.getResourceName()) + "_" + i + ".cpp";
                SpecsIo.write(new File(tempSrcs, newFilename), baseTest.read());
            }
        }

        var config = new TUPatcherConfig();

        List<File> files;
        // files = SpecsIo.getFiles(tempSrcs).stream().map(File::getAbsoluteFile).collect(Collectors.toList());
        files = SpecsIo.getFiles(tempSrcs);
        // files = Arrays.asList(SpecsIo.getFiles(tempSrcs).get(0));

        // config.set(TUPatcherConfig.SOURCE_PATHS, StringList.newInstance(tempSrcs.getAbsolutePath()));
        config.set(TUPatcherConfig.SOURCE_PATHS, StringList.newInstanceFromListOfFiles(files));
        // SpecsIo.getFiles(tempSrcs).stream().map(File::getAbsoluteFile).collect(Collectors.toList())));
        // SpecsIo.getFiles(tempSrcs)));
        // Arrays.asList(SpecsIo.getFiles(tempSrcs).get(0))));
        config.set(TUPatcherConfig.OUTPUT_FOLDER, tempOutput);
        config.set(TUPatcherConfig.PARALLEL, true);

        assertEquals(0, new TUPatcherLauncher(config).execute());
    }
}
