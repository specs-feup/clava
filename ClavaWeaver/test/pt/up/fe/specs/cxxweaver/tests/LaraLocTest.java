/**
 * Copyright 2019 SPeCS.
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

package pt.up.fe.specs.cxxweaver.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.lara.loc.LaraStats;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.providers.ResourceProvider;

public class LaraLocTest {

    @BeforeClass
    public static void init() {
        SpecsSystem.programStandardInit();
    }

    @Test
    public void test() {

        // Create file to test
        ResourceProvider laraResource = () -> "clava/test/loc/ClavaAspectsForLoc.lara";
        File tempFile = new File(SpecsIo.getTempFolder("laraloc"), "laraloc-test.lara");
        SpecsIo.write(tempFile, SpecsIo.read(SpecsIo.resourceToStream(laraResource)));

        LaraStats laracLoc = new LaraStats(CxxWeaver.buildLanguageSpecification());
        laracLoc.addFileStats(tempFile);

        assertEquals(Integer.valueOf(50), laracLoc.get(LaraStats.LARA_STMTS));
        assertEquals(Integer.valueOf(9), laracLoc.get(LaraStats.ASPECTS));
        assertEquals(Integer.valueOf(3), laracLoc.get(LaraStats.COMMENTS));
        assertEquals(Integer.valueOf(2), laracLoc.get(LaraStats.FUNCTIONS));

        // Clean
        SpecsIo.delete(tempFile);
    }

}
