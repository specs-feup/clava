/**
 * Copyright 2018 SPeCS.
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

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.lara.unit.LaraUnitReport;
import pt.up.fe.specs.lara.unit.LaraUnitTester;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class ClavaWeaverUnitTester {

    @Test
    public void test() {
        SpecsSystem.programStandardInit();
        LaraUnitTester laraUnitTester = new LaraUnitTester(new CxxWeaver(), false);

        // String baseFolder = "C:\\Users\\JoaoBispo\\Documents\\MEGA\\Work\\Tasks\\2018-02-19 LaraUnit Test With
        // Clava";
        // String baseFolder = "C:\\Users\\JoaoBispo\\Desktop\\shared\\repositories-programming\\specs-lara\\2018 DSD";
        String baseFolder = "C:\\Users\\JoaoBispo\\Desktop\\shared\\repositories-programming\\specs-lara\\2018-ASE";

        // String testFolder = "C:\\Users\\JoaoBispo\\Documents\\MEGA\\Work\\Tasks\\2018-02-19 LaraUnit Test With
        // Clava\\test";

        LaraUnitReport results = laraUnitTester.testFolder(new File(baseFolder),
                new File(baseFolder, "test/ParallelizationTest.lara"));

        SpecsLogs.msgInfo(results.getReport());
        assertTrue("Did not pass the tests", results.isSuccess());
        // fail("Not yet implemented");
    }

}
