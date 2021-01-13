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

package eu.antarex.clang.parser.tests;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.antarex.clang.parser.AClangAstTester;
import eu.antarex.clang.parser.CxxTester;

public class CxxIssuesTest {

    @BeforeClass
    public static void setup() throws Exception {
        AClangAstTester.clear();
    }

    @After
    public void tearDown() throws Exception {
        AClangAstTester.clear();
    }

    @Test
    public void testClavaIssue09() {
        new CxxTester("issues/clava_issue09.h").test();
    }

    @Test
    public void testClavaIssue10() {
        new CxxTester("issues/clava_issue10.cpp").test();
    }

    @Test
    public void testClavaIssue11() {
        new CxxTester("issues/clava_issue11.cpp").test();
    }

    @Test
    public void testClavaIssue13() {
        new CxxTester("issues/clava_issue13.cpp").showClavaAst().showCode().test();
    }

    @Test
    public void testClavaIssue14() {
        new CxxTester("issues/clava_issue14.h").test();
    }

    @Test
    public void testClavaIssue15() {
        new CxxTester("issues/clava_issue15.cpp").test();
    }

    @Test
    public void testClavaIssue17() {
        new CxxTester("issues/clava_issue17.cpp").test();
    }

    @Test
    public void testClavaIssue18() {
        new CxxTester("issues/clava_issue18.cpp").test();
    }

    @Test
    public void testClavaIssue19() {
        new CxxTester("issues/clava_issue19.cpp").test();
    }

    @Test
    public void testClavaIssue20() {
        new CxxTester("issues/clava_issue20.cpp").test();
    }

    @Test
    public void testClavaIssue21() {
        new CxxTester("issues/clava_issue21.cpp").test();
    }

    @Test
    public void testClavaIssue24() {
        new CxxTester("issues/clava_issue24.cpp")
                // .showCode()
                // .showClavaAst()
                // .onePass()
                .test();
    }
}
