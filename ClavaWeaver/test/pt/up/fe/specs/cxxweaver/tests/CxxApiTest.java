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
        // Disable syntax check of woven code, SpecsLogger includes are not available
        newTester().setCheckWovenCodeSyntax(false)
                .test("LoggerTestWithLib.lara", "logger_test.cpp");
    }

    @Test
    public void testEnergy() {
        // Disable syntax check of woven code, rapl include is not available
        newTester().setCheckWovenCodeSyntax(false)
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
        newTester()
                .set(CxxWeaverOption.PARSE_INCLUDES)
                .test("CMakerTest.lara", "cmaker_test.cpp", "cmaker_test.h");
    }

    @Test
    public void testMathExtra() {
        newTester().test("MathExtraTest.lara", "math_extra_test.cpp");
    }

    @Test
    public void testWeaverLauncher() {
        newTester().test("WeaverLauncherTest.lara", "weaver_launcher_test.cpp");
    }

    @Test
    public void testClavaDataStore() {
        newTester().test("ClavaDataStoreTest.lara", "clava_data_store_test.cpp");
    }

    @Test
    public void testUserValues() {
        newTester().test("UserValuesTest.lara", "user_values.cpp");
    }

    @Test
    public void testClavaCode() {
        newTester().test("ClavaCodeTest.lara", "clava_code.cpp");
    }

    @Test
    public void testClavaJoinPointsTest() {
        newTester().test("ClavaJoinPointsTest.lara", "clava_join_points.cpp");
    }

    @Test
    public void testJpFilter() {
        newTester()
                .set(CxxWeaverOption.PARSE_INCLUDES)
                .test("JpFilter.lara", "jp_filter.hpp");
    }

    @Test
    public void testRebuild() {
        newTester().test("RebuildTest.lara", "rebuild.cpp");
    }

    @Test
    public void testFileIterator() {
        newTester().test("FileIteratorTest.lara", "file_iterator_1.cpp", "file_iterator_2.cpp");
    }

    @Test
    public void testAddHeaderFile() {
        newTester().test("AddHeaderFileTest.lara", "add_header_file.h");
    }

    @Test
    public void testClava() {
        newTester().test("ClavaTest.lara", "clava.cpp");
    }

    @Test
    public void testQuery() {
        newTester().test("QueryTest.lara", "query.cpp");
    }

    @Test
    public void testLaraCommonLanguage() {
        newTester().test("LaraCommonLanguageTest.lara", "lara_common_language.cpp");
    }

    @Test
    public void testClavaType() {
        newTester().test("ClavaTypeTest.lara");
    }

    @Test
    public void testStatementDecomposer() {
        newTester().test("StatementDecomposerTest.lara", "stmt_decomposer.cpp");
    }

    // @Test
    public void testCode2Vec() {
        newTester().test("Code2VecTest.js", "code2vec.cpp");
    }

    // @Test
    public void testSimplifyVarDeclarations() {
        newTester().test("PassSimplifyVarDeclarations.lara", "pass_simplify_var_declarations.cpp");
    }
}
