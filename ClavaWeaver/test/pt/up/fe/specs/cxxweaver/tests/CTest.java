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

import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.weaver.options.CxxWeaverOption;
import pt.up.fe.specs.cxxweaver.ClavaWeaverTester;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class CTest {

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
        return new ClavaWeaverTester("clava/test/weaver/", Standard.C99)
                .setResultPackage("c/results")
                .setSrcPackage("c/src");
    }

    @Test
    public void testLoop() {
        newTester().test("Loop.lara", "loop.c");
    }

    @Test
    public void testReplaceCallWithStmt() {
        newTester().test("ReplaceCallWithStmt.lara", "ReplaceCallWithStmt.c");
    }

    @Test
    public void testInsertsLiteral() {
        newTester().test("InsertsLiteral.lara", "inserts.c");
    }

    @Test
    public void testInsertsJp() {
        newTester().test("InsertsJp.lara", "inserts.c");
    }

    @Test
    public void testClone() {
        newTester().test("Clone.lara", "clone.c");
    }

    @Test
    public void testAddGlobal() {
        newTester().test("AddGlobal.lara", "add_global_1.c", "add_global_2.c");
    }

    @Test
    public void testExpressions() {
        newTester().test("Expressions.lara", "expressions.c");
    }

    @Test
    public void testDijkstra() {
        newTester().setCheckWovenCodeSyntax(false).test("Dijkstra.lara", "dijkstra.c");
        newTester().setCheckWovenCodeSyntax(false).test("Dijkstra.lara", "dijkstra.c");
    }

    @Test
    public void testWrap() {
        newTester()
                .set(CxxWeaverOption.PARSE_INCLUDES)
                .test("Wrap.lara", "wrap.c", "wrap.h");
    }

    @Test
    public void testVarrefInWhile() {
        newTester().test("VarrefInWhile.lara", "varref_in_while.c");
    }

    @Test
    public void testInline() {
        newTester().test("Inline.lara", "inline.c", "inline_utils.h", "inline_utils.c");
    }

    @Test
    public void testSetType() {
        newTester().test("SetType.lara", "set_type.c");
    }

    @Test
    public void testDetach() {
        newTester().test("Detach.lara", "detach.c");
    }

    @Test
    public void testInlineNasLu() {
        if (SpecsSystem.isWindows()) {
            SpecsLogs.info("Skipping test, does not work on Windows");
            return;
        }

        newTester().test("InlineNasLu.lara", "inline_nas_lu.c");
    }

    @Test
    public void testInlineNasFt() {
        newTester().test("InlineNasFt.lara", "inline_nas_ft.c");
    }

    @Test
    public void testNullNodes() {
        newTester().test("NullNodes.lara", "null_nodes.c");
    }

    @Test
    public void testTypeRenamer() {
        newTester().test("TypeRenamer.lara", "type_renamer.c");
    }

    @Test
    public void testAstNodes() {
        newTester().test("AstNodes.lara", "ast_nodes.c");
    }

    @Test
    public void testRemoveInclude() {
        newTester().test("RemoveInclude.lara", "remove_include.c", "remove_include_0.h", "remove_include_1.h",
                "remove_include_2.h");
    }

    @Test
    public void testDynamicCallGraph() {
        newTester().test("DynamicCallGraph.lara", "dynamic_call_graph.c");
    }

    @Test
    public void testSelects() {
        newTester().test("Selects.lara", "selects.c");
    }

    @Test
    public void testScope() {
        newTester().test("Scope.lara", "scope.c");
    }

    @Test
    public void testArray() {
        newTester().test("ArrayTest.lara", "array_test.c");
    }

    // @Test
    public void testOpenCLType() {
        // TODO: Certain attributes are not supported yet (e.g., ReqdWorkGroupSizeAttr, WorkGroupSizeHintAttr,
        // VecTypeHintAttr)
        newTester().set(ClavaOptions.STANDARD, Standard.OPENCL20).test("OpenCLType.lara", "opencl_type.cl");
    }

    @Test
    public void testCilk() {
        // Generated code has Cilk directives
        newTester().set(ClavaOptions.FLAGS, "-fcilkplus").test("Cilk.lara", "cilk.c");
    }

    @Test
    public void testTagDecl() {
        newTester().test("TagDecl.lara", "tag_decl.c");
    }

    @Test
    public void testFile() {
        newTester().test("File.lara", "file.c");
    }

    @Test
    public void testSwitch() {
        newTester().test("SwitchTest.lara", "switch.c");
    }

    @Test
    public void testAddParam() {
        newTester().test("AddParamTest.lara", "add_param.c");
    }

    @Test
    public void testAddArg() {
        newTester().test("AddArgTest.lara", "add_arg.c");
    }

    @Test
    public void testCfg() {
        newTester().test("Cfg.lara", "cfg.c");
    }
}
