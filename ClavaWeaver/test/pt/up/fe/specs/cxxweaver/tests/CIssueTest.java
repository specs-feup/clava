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
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.cxxweaver.ClavaWeaverTester;
import pt.up.fe.specs.util.SpecsSystem;

public class CIssueTest {

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
        return new ClavaWeaverTester("clava/test/issues/", Standard.C99)
                .setResultPackage("c/results")
                .setSrcPackage("c/src");
    }


    @Test
    public void testIssue168() {
        newTester().test("Issue168.mjs", "issue_168.c");
    }


    @Test
    public void testIssueAiq1() {
        newTester().test("Issue_aiq_1.mjs", "issue_aiq_1.c");
    }

    @Test
    public void testIssue187() {
        newTester().test("Issue187.mjs", "issue_187.c");
    }
}
