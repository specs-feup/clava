/**
 * Copyright 2016 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.cxxweaver.tests;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lara.interpreter.joptions.config.interpreter.LaraiKeys;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.cxxweaver.ClavaWeaverTester;
import pt.up.fe.specs.lang.SpecsPlatforms;
import pt.up.fe.specs.util.SpecsSystem;

public class CApiTest {

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
        return new ClavaWeaverTester("clava/test/api/", Standard.C99)
                .setSrcPackage("c/src/")
                .setResultPackage("c/results/");
    }

    @Test
    public void testLogger() {
        newTester().test("LoggerTest.lara", "logger_test.c");
    }

    @Test
    public void testTimer() {
        ClavaWeaverTester tester = newTester();
        if (SpecsPlatforms.isUnix()) {
            tester.setResultsFile("TimerTest.lara.unix.txt");
        }

        tester.test("TimerTest.lara", "timer_test.c");
    }

    /**
     * Compiles C code, but with C++ flag.
     */
    @Test
    public void testTimerWithCxxFlag() {
        ClavaWeaverTester tester = newTester();
        if (SpecsPlatforms.isUnix()) {
            // Test not working on Unix
            return;
            // tester.set(ClavaOptions.STANDARD, Standard.C11).setResultsFile("TimerTest.lara.unix.txt");
        }

        tester.set(ClavaOptions.STANDARD, Standard.CXX11).test("TimerTest.lara", "timer_test.c");
    }

    @Test
    public void testEnergy() {
        // Disable syntax check of woven code, rapl include is not available
        newTester().setCheckWovenCodeSyntax(false)
                .test("EnergyTest.lara", "energy_test.c");
    }

    @Test
    public void testAutoPar() {
        if (SpecsPlatforms.isUnix()) {
            // Enable restrict mode, to test if it works
            // Using AutoPar example since it needs to call an external program
            newTester().set(LaraiKeys.RESTRICT_MODE, Boolean.TRUE)
                    .test("AutoParTest.lara", "autopar_test.c");
        }

    }

    @Test
    public void testCodeInserter() {
        newTester()
                // .set(LaraiKeys.DEBUG_MODE)
                // .set(LaraiKeys.VERBOSE, VerboseLevel.all)
                .test("CodeInserterTest.js", "code_inserter.c");
    }

    @Test
    public void testArrayLinearizer() {
        // newTester().test("ArrayLinearizerTest.lara", "2d_array.c");
        // newTester().test("ArrayLinearizerTest.lara", "3d_array.c");
        newTester().test("ArrayLinearizerTest.lara", "qr.c");
    }

    @Test
    public void testSelector() {
        newTester().test("SelectorTest.lara", "selector_test.c");
    }

    @Test
    public void testAutoParInline() {
        // TODO: No expected output, code has bugs to solve - does not take into account existing variable names
        newTester().test("AutoParInlineTest.lara", "autopar_inline.c");
    }

    @Test
    public void testHls() {
        newTester().test("HlsTest.lara", "hls.c");
    }

    @Test
    public void testStrcpyChecker() {
        newTester().test("StrcpyChecker.lara", "strcpy.c");
    }

    @Test
    public void testStaticCallGraph() {
        newTester().test("StaticCallGraphTest.js", "static_call_graph.c");
    }

    @Test
    public void testPassComposition() {
        newTester().test("PassCompositionTest.js", "pass_composition.c");
    }

    @Test
    public void testCfgApi() {
        newTester().test("CfgApi.js", "cfg_api.c");
    }

    @Test
    public void testInliner() {
        newTester()
                // .setCheckWovenCodeSyntax(false)
                .test("InlinerTest.js", "inliner.c");
    }

    @Test
    public void testStatementDecomposer() {
        newTester().test("StatementDecomposerTest.js", "stmt_decomposer.c");
    }

    @Test
    public void testToSingleFile() {
        newTester().test("ToSingleFile.js", "to_single_file_1.c", "to_single_file_2.c");
    }


    @Test
    public void testLivenessAnalysis() {
        newTester().test("LivenessAnalysisTest.js", "liveness_analysis.c");
    }


    @Test
    public void testSwitchToIf() {
        newTester().test("SwitchToIfTransformationTest.js", "switch_to_if.c");
    }

}
