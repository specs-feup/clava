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

public class CxxProblematicTester {

    @BeforeClass
    public static void setup() throws Exception {
        AClangAstTester.clear();
    }

    @After
    public void tearDown() throws Exception {
        AClangAstTester.clear();
    }

    @Test
    public void testBoost() {
        // Jenkins machine does not have boost installed
        new CxxTester("boost.cpp").test();
    }

    @Test
    public void testOperator() {
        // GCC on Jenkins machine does not support .operator bool()
        new CxxTester("operator.cpp").test();
    }

    @Test
    public void testClasses() {
        // Unimplemented features
        new CxxTester("classes.cpp").test();
    }

    @Test
    public void testLambda() {
        // Lambdas not implemented
        // Reference for tests: https://msdn.microsoft.com/en-us/library/dd293608.aspx
        new CxxTester("lambda.cpp")
                .addFlags("-std=c++14")
                .test();
    }

    @Test
    public void testTemplateAuto() {
        // FieldDeclParser, consumes name but has no name
        new CxxTester("problematic/template_auto.cpp").test();
    }

    @Test
    public void testImplicitReference() {
        // FieldDeclParser, consumes name but has no name
        new CxxTester("problematic/implicit_reference.cpp").test();
    }

    @Test
    public void testSortedId() {
        new CxxTester("bench/sorted_id.cpp", "bench/sorted_id.h").test();
    }

    @Test
    public void testDummy() {
        new CxxTester("problematic/dummy.cpp").showCode().showClavaAst().test();
        // onePass().showCode().showClangAst().showClavaAst()
    }

}
