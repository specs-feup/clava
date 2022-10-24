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

public class CxxBenchTest {

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
        return new ClavaWeaverTester("clava/test/bench/", Standard.CXX11)
                .setResultPackage("cpp/results")
                .setSrcPackage("cpp/src");
    }

    @Test
    public void testLoicEx1() {
        newTester().test("LoicEx1.lara", "loic_ex1.cpp");
    }

    @Test
    public void testLoicEx2() {
        newTester().setCheckWovenCodeSyntax(false).checkExpectedOutput(false).test("LoicEx2.lara", "loic_ex2.cpp");
    }

    @Test
    public void testLoicEx3() {
        // newTester().setCheckWovenCodeSyntax(false).test("LoicEx3.lara", "loic_ex3.cpp");
        newTester().test("LoicEx3.lara", "loic_ex3.cpp");
    }

    @Test
    public void testLSIssue2() {
        newTester().test("LSIssue2.lara", "ls_issue2.cpp");
    }

    @Test
    public void testCbMultios() {
        newTester().test("CbMultios.lara", "cb_multios.cpp");
    }
}
