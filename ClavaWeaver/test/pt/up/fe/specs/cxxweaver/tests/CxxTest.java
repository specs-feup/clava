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
        newTester().test("Statement.lara", "statement.cpp");
    }

    @Test
    public void testLoop() {
        newTester().test("Loop.lara", "loop.cpp");
    }

    @Test
    public void testReplaceCallWithStmt() {
        newTester().test("ReplaceCallWithStmt.lara", "ReplaceCallWithStmt.cpp");
    }

    @Test
    public void testInsertsLiteral() {
        newTester().test("InsertsLiteral.lara", "inserts.cpp");
    }

    @Test
    public void testInsertsJp() {
        newTester().test("InsertsJp.lara", "inserts.cpp");
    }

    @Test
    public void testPragmas() {
        newTester().test("Pragmas.lara", "pragma.cpp");
    }

    @Test
    public void testActions() {
        newTester().test("Actions.lara", "actions.cpp");
    }

    @Test
    public void testArrayAccess() {
        newTester().test("ArrayAccess.lara", "array_access.cpp", "array_access.h");
    }

    @Test
    public void testAttributeUse() {
        newTester().test("AttributeUse.lara", "attribute_use.cpp");
    }

    // @Test
    // public void testHdf5Types() {
    // new newTester().test("Hdf5Types.lara", "hdf5types.cpp");
    // }

    @Test
    public void testOmpThreadsExplore() {
        newTester().test("OmpThreadsExplore.lara", "omp_threads_explore.cpp");
    }

    @Test
    public void testHamidCfg() {
        newTester().test("HamidCfg.lara", "dijkstra.cpp");
    }

    @Test
    public void testClone() {
        newTester().test("Clone.lara", "clone.cpp");
    }

    @Test
    public void testAddGlobal() {
        newTester().test("AddGlobal.lara", "add_global_1.cpp", "add_global_2.cpp");
    }

    @Test
    public void testOmp() {
        newTester().test("Omp.lara", "omp.cpp");
    }

    @Test
    public void testOmpAttributes() {
        newTester().test("OmpAttributes.lara", "omp_attributes.cpp");
    }

    @Test
    public void testOmpSetAttributes() {
        newTester().test("OmpSetAttributes.lara", "omp_set_attributes.cpp");
    }

    @Test
    public void testExpressions() {
        newTester().test("Expressions.lara", "expressions.cpp", "classA.h");
    }

    @Test
    public void testParentRegion() {
        newTester().test("ParentRegion.lara", "parent_region.cpp");
    }

    @Test
    public void testVarDecl() {
        newTester().test("Vardecl.lara", "vardecl.cpp");
    }

    @Test
    public void testParamType() {
        newTester().test("ParamType.lara", "param_type.cpp");
    }

    @Test
    public void testWrap() {
        // newTester().test("Wrap.lara", "wrap.cpp", "wrap.h", "lib/lib.h", "lib/lib.cpp");
        newTester().test("Wrap.lara", "wrap.cpp", "wrap.h");
    }

    @Test
    public void testSelectVarDecl() {
        newTester().test("SelectVardecl.lara", "select_vardecl.cpp");
    }

    @Test
    public void testMacros() {
        newTester().setCheckWovenCodeSyntax(false).test("Macros.lara", "macros.cpp");
    }

    @Test
    public void testCall() {
        newTester().test("Call.lara", "call.cpp");
    }

    @Test
    public void testPragmaClavaAttribute() {
        newTester().test("PragmaAttribute.lara", "pragma_attribute.cpp");
    }

    @Test
    public void testTypeTemplate() {
        newTester().test("TypeTemplate.lara", "type_template.cpp");
    }

    @Test
    public void testFunction() {
        newTester().test("Function.lara", "function.cpp", "function.h");
    }

    @Test
    public void testAstAttributes() {
        newTester().test("AstAttributes.lara", "ast_attributes.cpp");
    }

    // @Test
    // public void testClass() {
    // newTester().test("Class.lara", "class.cpp");
    // }

    @Test
    public void testPragmaData() {
        newTester().test("PragmaData.lara", "pragma_data.cpp");
    }

    @Test
    public void testGlobalAttributes() {
        newTester().test("GlobalAttributes.lara", "global_attributes.cpp");
    }

}
