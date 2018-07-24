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
import eu.antarex.clang.parser.CTester;
import pt.up.fe.specs.lang.SpecsPlatforms;

public class CBenchTest {

    @BeforeClass
    public static void setup() throws Exception {
        AClangAstTester.clear();
    }

    @After
    public void tearDown() throws Exception {
        AClangAstTester.clear();
    }

    @Test
    public void testBt() {
        if (SpecsPlatforms.isLinux()) {
            new CTester("bench/nas_bt.c").test();
        }
    }

    @Test
    public void testUa() {
        if (SpecsPlatforms.isLinux()) {
            new CTester("bench/nas_ua.c").test();
        }
    }

    @Test
    public void testLu() {
        new CTester("bench/nas_lu.c").test();
    }

    /** NOT WORKING **/
    @Test
    public void testFt() {
        new CTester("bench/nas_ft.c").test();
    }

}
