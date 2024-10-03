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

package pt.up.fe.specs.cxxweaver.tests;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.weaver.options.CxxWeaverOption;
import pt.up.fe.specs.cxxweaver.ClavaWeaverTester;
import pt.up.fe.specs.util.SpecsSystem;

public class CBenchTest {

    @BeforeClass
    public static void setupOnce() {
        SpecsSystem.programStandardInit();
        ClavaWeaverTester.clean();
    }

    @After
    public void tearDown() {
        ClavaWeaverTester.clean();
    }

    private static ClavaWeaverTester newTester() {
        return new ClavaWeaverTester("clava/test/bench/", Standard.C99)
                .setResultPackage("c/results")
                .setSrcPackage("c/src");
    }

    @Test
    public void testHamidRegion() {
        newTester()
                .set(CxxWeaverOption.PARSE_INCLUDES)
                .test("HamidRegion.js", "hamid_region.c", "hamid_region.h");
    }

    @Test
    public void testDspMatmul() {
        newTester().test("DspMatmul.js", "dsp_matmul.c");
    }

    @Test
    public void testLSIssue1() {
        newTester().test("LSIssue1.js", "ls_issue1.c");
    }
}
