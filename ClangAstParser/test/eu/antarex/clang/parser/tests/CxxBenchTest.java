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

public class CxxBenchTest {

    @BeforeClass
    public static void setup() throws Exception {
        AClangAstTester.clear();
    }

    @After
    public void tearDown() throws Exception {
        AClangAstTester.clear();
    }

    @Test
    public void testBlockedMm() {
        new CxxTester("bench/blocked_mm.cpp").test();
    }

    @Test
    public void testFastStack() {
        new CxxTester("bench/fast_stack.cpp").showClangDump().test();
    }

    @Test
    public void testSortedId() {
        new CxxTester("bench/sorted_id.cpp", "bench/sorted_id.h").showCode().onePass().test();
    }

}
