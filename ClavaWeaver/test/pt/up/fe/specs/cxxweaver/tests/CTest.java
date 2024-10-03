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

    // TODO: Temporarily disabled, Jenkins fails with "Cannot inherit from final class"
    // @Test
    public void testLoop() {
        newTester().test("Loop.js", "loop.c");
    }

    @Test
    public void testReplaceCallWithStmt() {
        newTester().test("ReplaceCallWithStmt.js", "ReplaceCallWithStmt.c");
    }

    @Test
    public void testInsertsLiteral() {
        newTester().test("InsertsLiteral.js", "inserts.c");
    }

    @Test
    public void testInsertsJp() {
        newTester().test("InsertsJp.js", "inserts.c");
    }

    @Test
    public void testClone() {
        newTester().test("Clone.js", "clone.c");
    }

    @Test
    public void testAddGlobal() {
        newTester().test("AddGlobal.js", "add_global_1.c", "add_global_2.c");
    }

    @Test
    public void testExpressions() {
        newTester().test("Expressions.js", "expressions.c");
    }

    @Test
    public void testDijkstra() {
        newTester().setCheckWovenCodeSyntax(false).checkExpectedOutput(false).test("Dijkstra.js", "dijkstra.c");
    }

    @Test
    public void testWrap() {
        newTester()
                .set(CxxWeaverOption.PARSE_INCLUDES)
                .test("Wrap.js", "wrap.c", "wrap.h");
    }

    @Test
    public void testVarrefInWhile() {
        newTester().test("VarrefInWhile.js", "varref_in_while.c");
    }

    @Test
    public void testInline() {
        newTester().test("Inline.js", "inline.c", "inline_utils.h", "inline_utils.c");
    }

    @Test
    public void testSetType() {
        newTester().test("SetType.js", "set_type.c");
    }

    @Test
    public void testDetach() {
        newTester().test("Detach.js", "detach.c");
    }

    @Test
    public void testInlineNasLu() {
        if (SpecsSystem.isWindows()) {
            SpecsLogs.info("Skipping test, does not work on Windows");
            return;
        }

        newTester().checkExpectedOutput(false).test("InlineNasLu.js", "inline_nas_lu.c");
    }

    @Test
    public void testInlineNasFt() {
        newTester().checkExpectedOutput(false).test("InlineNasFt.js", "inline_nas_ft.c");
    }

    @Test
    public void testNullNodes() {
        newTester().test("NullNodes.js", "null_nodes.c");
    }

    @Test
    public void testTypeRenamer() {
        newTester().test("TypeRenamer.js", "type_renamer.c");
    }

    @Test
    public void testAstNodes() {
        newTester().test("AstNodes.js", "ast_nodes.c");
    }

    @Test
    public void testRemoveInclude() {
        newTester().test("RemoveInclude.js", "remove_include.c", "remove_include_0.h", "remove_include_1.h",
                "remove_include_2.h");
    }

    @Test
    public void testDynamicCallGraph() {
        newTester().test("DynamicCallGraph.js", "dynamic_call_graph.c");
    }

    @Test
    public void testSelects() {
        newTester().test("Selects.js", "selects.c");
    }

    @Test
    public void testScope() {
        newTester().test("Scope.js", "scope.c");
    }

    @Test
    public void testArray() {
        newTester().test("ArrayTest.js", "array_test.c");
    }

    // @Test
    public void testOpenCLType() {
        // TODO: Certain attributes are not supported yet (e.g., ReqdWorkGroupSizeAttr, WorkGroupSizeHintAttr,
        // VecTypeHintAttr)
        newTester().set(ClavaOptions.STANDARD, Standard.OPENCL20).test("OpenCLType.js", "opencl_type.cl");
    }

    @Test
    public void testCilk() {
        // Generated code has Cilk directives
        newTester().set(ClavaOptions.FLAGS, "-fcilkplus").test("Cilk.js", "cilk.c");
    }

    @Test
    public void testTagDecl() {
        newTester().test("TagDecl.js", "tag_decl.c");
    }

    @Test
    public void testFile() {
        newTester().test("File.js", "file.c");
    }

    @Test
    public void testSwitch() {
        newTester().test("SwitchTest.js", "switch.c");
    }

    @Test
    public void testAddParam() {
        newTester().test("AddParamTest.js", "add_param.c");
    }

    @Test
    public void testAddArg() {
        newTester().test("AddArgTest.js", "add_arg.c");
    }

    @Test
    public void testCfg() {
        newTester().test("Cfg.js", "cfg.c");
    }

    @Test
    public void testExprStmt() {
        newTester().test("ExprStmt.js", "expr_stmt.c");
    }

    @Test
    public void testTraversal() {
        newTester().test("Traversal.js", "traversal.c");
    }

    @Test
    public void testIf() {
        newTester().test("If.js", "if.c");
    }
}
