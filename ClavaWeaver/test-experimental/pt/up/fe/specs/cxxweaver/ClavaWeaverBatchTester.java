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

package pt.up.fe.specs.cxxweaver;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.options.CxxWeaverOption;
import pt.up.fe.specs.larai.WeaverBatchTester;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

public class ClavaWeaverBatchTester {

    @BeforeClass
    public static void setupOnce() {
        SpecsSystem.programStandardInit();
    }

    @Test
    public void test() {
        String foldername = "C:\\Users\\JoaoBispo\\Desktop\\shared\\specs-lara\\2017 COMLAN\\";
        File baseFolder = SpecsIo.existingFolder(foldername);
        WeaverBatchTester.testWithDataStore(baseFolder, "clava", () -> new CxxWeaver(),
                ClavaWeaverBatchTester::processDataStore);
    }

    private static void processDataStore(DataStore dataStore) {
        // Activate check syntex
        // System.out.println("CHECK SYNTAX BEFORE:" + dataStore.get(CxxWeaverOption.CHECK_SYNTAX));

        // dataStore.set(CxxWeaverOption.DISABLE_CLAVA_INFO, false);

        // Do not enable check syntax with measure energy
        File configFile = dataStore.get(WeaverBatchTester.CURRENT_CONFIGURATION_FILE);
        if (configFile.getName().startsWith("MeasureEnergy")) {
            return;
        }
        dataStore.set(CxxWeaverOption.CHECK_SYNTAX, true);
        // System.out.println("CHECK SYNTAX AFTER:" + dataStore.get(CxxWeaverOption.CHECK_SYNTAX));

    }

}
