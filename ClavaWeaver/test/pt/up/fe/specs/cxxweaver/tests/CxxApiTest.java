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
import pt.up.fe.specs.cxxweaver.ClavaWeaverTester;
import pt.up.fe.specs.util.SpecsSystem;

public class CxxApiTest {

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
        return new ClavaWeaverTester("clava/test/api/", Standard.CXX11)
                .setSrcPackage("cpp/src/")
                .setResultPackage("cpp/results/");
    }

    @Test
    public void testLogger() {
        newTester().test("LoggerTest.lara", "logger_test.cpp");
    }

    @Test
    public void testLoggerWithLibrary() {
        // Disable syntax check of weaved code, SpecsLogger includes are not available
        newTester().setCheckWeavedCodeSyntax(false)
                .test("LoggerTestWithLib.lara", "logger_test.cpp");
    }

    @Test
    public void testEnergy() {
        // Disable syntax check of weaved code, rapl include is not available
        newTester().setCheckWeavedCodeSyntax(false)
                .test("EnergyTest.lara", "energy_test.cpp");
    }

    @Test
    public void testTimer() {
        newTester().test("TimerTest.lara", "timer_test.cpp");
    }

    @Test
    public void testClavaFindJp() {
        newTester().test("ClavaFindJpTest.lara", "clava_find_jptest.cpp");
    }

    @Test
    public void testCMaker() {
        newTester().test("CMakerTest.lara", "cmaker_test.cpp", "cmaker_test.h");
    }
}
