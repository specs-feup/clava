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
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class CxxTest {

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
        return new ClavaWeaverTester("clava/test/weaver/", Standard.CXX11)
                .setResultPackage("cpp/results")
                .setSrcPackage("cpp/src");
    }

    @Test
    public void testStatement() {
        newTester().test("Statement.js", "statement.cpp");
    }

    @Test
    public void testLoop() {
        newTester().test("Loop.js", "loop.cpp");
    }

    @Test
    public void testReplaceCallWithStmt() {
        newTester().test("ReplaceCallWithStmt.js", "ReplaceCallWithStmt.cpp");
    }

    @Test
    public void testInsertsLiteral() {
        newTester().test("InsertsLiteral.js", "inserts.cpp");
    }

    @Test
    public void testInsertsJp() {
        newTester().test("InsertsJp.js", "inserts.cpp");
    }

    @Test
    public void testPragmas() {
        newTester().test("Pragmas.js", "pragma.cpp");
    }

    @Test
    public void testPragmas2() {
        newTester().test("Pragma2.js", "pragma2.cpp");
    }

    @Test
    public void testActions() {
        newTester().test("Actions.js", "actions.cpp");
    }

    @Test
    public void testArrayAccess() {
        newTester().test("ArrayAccess.js", "array_access.cpp", "array_access.h");
    }

    @Test
    public void testAttributeUse() {
        newTester().test("AttributeUse.js", "attribute_use.cpp");
    }

    @Test
    public void testHdf5Types() {
        newTester()
                // Disable syntax checking, since test system may not have HDF5 includes automatically available
                .setCheckWovenCodeSyntax(false)
                .set(CxxWeaverOption.PARSE_INCLUDES)
                .test("Hdf5Types.js", "hdf5types.cpp");
    }

    @Test
    public void testOmpThreadsExplore() {
        newTester().test("OmpThreadsExplore.js", "omp_threads_explore.cpp");
    }

    @Test
    public void testHamidCfg() {
        newTester().test("HamidCfg.js", "dijkstra.cpp");
    }

    @Test
    public void testClone() {
        newTester()
                .set(CxxWeaverOption.PARSE_INCLUDES)
                .test("Clone.js", "clone.cpp", "clone.h");
    }

    @Test
    public void testAddGlobal() {
        newTester().test("AddGlobal.js", "add_global_1.cpp", "add_global_2.cpp");
    }

    @Test
    public void testOmp() {
        newTester().test("Omp.js", "omp.cpp");
    }

    @Test
    public void testOmpAttributes() {
        newTester().test("OmpAttributes.js", "omp_attributes.cpp");
    }

    @Test
    public void testOmpSetAttributes() {
        newTester().test("OmpSetAttributes.js", "omp_set_attributes.cpp");
    }

    @Test
    public void testExpressions() {
        newTester().test("Expressions.js", "expressions.cpp", "classA.h");
    }

    @Test
    public void testParentRegion() {
        newTester().test("ParentRegion.js", "parent_region.cpp");
    }

    @Test
    public void testVarDecl() {
        newTester().test("Vardecl.js", "vardecl.cpp");
    }

    @Test
    public void testParamType() {
        newTester().checkExpectedOutput(false).test("ParamType.js", "param_type.cpp");
    }

    @Test
    public void testWrap() {
        if (SpecsSystem.isWindows()) {
            SpecsLogs.info("Skipping test, results are different on Windows");
            return;
        }

        // newTester().test("Wrap.js", "wrap.cpp", "wrap.h", "lib/lib.h", "lib/lib.cpp");
        newTester()
                .set(CxxWeaverOption.PARSE_INCLUDES)
                .test("Wrap.js", "wrap.cpp", "wrap.h");
    }

    @Test
    public void testSelectVarDecl() {
        newTester().test("SelectVardecl.js", "select_vardecl.cpp");
    }

    @Test
    public void testMacros() {
        newTester().setCheckWovenCodeSyntax(false).test("Macros.js", "macros.cpp");
    }

    @Test
    public void testCall() {
        newTester().test("Call.js", "call.cpp");
    }

    @Test
    public void testPragmaClavaAttribute() {
        newTester().test("PragmaAttribute.js", "pragma_attribute.cpp");
    }

    @Test
    public void testTypeTemplate() {
        newTester().test("TypeTemplate.js", "type_template.cpp");
    }

    @Test
    public void testFunction() {
        newTester().test("Function.js", "function.cpp", "function.h");
    }

    @Test
    public void testAstAttributes() {
        newTester().test("AstAttributes.js", "ast_attributes.cpp");
    }

    // @Test
    // public void testClass() {
    // newTester().test("Class.js", "class.cpp");
    // }

    @Test
    public void testPragmaData() {
        newTester().test("PragmaData.js", "pragma_data.cpp", "pragma_data_2.cpp");
    }

    @Test
    public void testGlobalAttributes() {
        newTester().test("GlobalAttributes.js", "global_attributes.cpp");
    }

    @Test
    public void testSetType() {
        newTester().test("SetTypeCxx.js", "set_type.cpp");
    }

    @Test
    public void testMultiFile() {
        newTester()
                .set(CxxWeaverOption.PARSE_INCLUDES)
                .test("MultiFile.js", "multiFile.cpp", "multiFile.h");
    }

    @Test
    public void testField() {
        newTester().test("Field.js", "field.hpp");
    }

    @Test
    public void testFileRebuild() {
        newTester()
                .set(CxxWeaverOption.PARSE_INCLUDES)
                .test("FileRebuild.js", "file_rebuild.cpp", "file_rebuild.h", "file_rebuild_2.h");
    }

    @Test
    public void testSetters() {
        newTester().test("Setters.js", "setters.cpp");
    }

    @Test
    public void testSkipParsingHeaders() {
        newTester().test("SkipParsingHeaders.js", "skip_parsing_headers.cpp", "skip_parsing_headers.h");
    }

    @Test
    public void testNoParsing() {
        newTester().test("NoParsing.js");
    }

    @Test
    public void testLaraGetter() {
        newTester()
                // .set(LaraiKeys.DEBUG_MODE)
                // .set(LaraiKeys.VERBOSE, VerboseLevel.all)
                // .set(LaraiKeys.TRACE_MODE, false)
                .test("LaraGetter.js");
    }

    @Test
    public void testVarDeclV2() {
        newTester().test("VardeclV2.js", "vardeclv2.cpp", "vardeclv2_2.cpp");
    }

    @Test
    public void testFile() {
        newTester().test("File.js", "file.cpp");
    }

    @Test
    public void testDataClass() {
        newTester().test("DataClass.js", "dataclass.cpp");
    }

    @Test
    public void testClassManipulation() {
        newTester()
                .set(CxxWeaverOption.PARSE_INCLUDES)
                .test("ClassManipulation.js", "class_manipulation.cpp", "class_manipulation.h");
    }

    @Test
    public void testThis() {
        newTester().test("ThisTest.js", "this.cpp");
    }

    @Test
    public void testMember() {
        newTester().test("Member.js", "member.cpp");
    }

    @Test
    public void testFieldRef() {
        newTester().checkExpectedOutput(false).test("FieldRef.js", "fieldRef.cpp");
    }

    @Test
    public void testExpressionDecls() {
        newTester().test("ExpressionDecls.js", "expressionDecls.cpp");
    }

    @Test
    public void testTemplateSpecializationType() {
        newTester().test("TemplateSpecializationType.js", "template_specialization_type.cpp");
    }

    @Test
    public void testReverseIterator() {
        newTester().test("ReverseIterator.js", "reverse_iterator.cpp");
    }

    @Test
    public void testFunction2() {
        newTester().test("Function2.js", "function2.cpp");
    }

    @Test
    public void testCloneOnFile() {
        newTester()
                // Generates a file in another folder, needs to generate the header file otherwise it will not parse
                // correctly the second time
                .set(CxxWeaverOption.PARSE_INCLUDES)
                .test("CloneOnFile.js", "clone_on_file.cpp",
                        "clone_on_file.h");
    }

    @Test
    public void testEmptyStmt() {
        newTester().test("EmptyStmt.js", "empty_stmt.cpp");
    }

    @Test
    public void testClass() {
        newTester().test("Class.js", "class.cpp");
    }

    @Test
    public void testCanonical() {
        newTester().test("CanonicalTest.js", "canonical.cpp");
    }

    @Test
    public void testBreak() {
        newTester().test("Break.js", "break.cpp");
    }
}
