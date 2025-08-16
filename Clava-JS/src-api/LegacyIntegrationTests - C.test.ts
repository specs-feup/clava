import { ClavaLegacyTester } from "../jest/ClavaLegacyTester.js";
import JavaTypes from "@specs-feup/lara/api/lara/util/JavaTypes.js";
import ClavaJavaTypes from "@specs-feup/clava/api/clava/ClavaJavaTypes.js";
import path from "path";
import "@specs-feup/clava/api/Joinpoints.js";

const isWindows = process.platform === "win32";

/* eslint-disable jest/expect-expect */
describe("CTest", () => {
    function newTester() {
        return new ClavaLegacyTester(
            path.resolve("../ClavaWeaver/resources/clava/test/weaver/"),
            ClavaJavaTypes.Standard.C99
        )
            .setResultPackage("c/results")
            .setSrcPackage("c/src");
    }

    // TODO: Temporarily disabled, Jenkins fails with "Cannot inherit from final class"
    it("Loop", async () => {
        await newTester().test("Loop.js", "loop.c");
    });

    it("ReplaceCallWithStmt", async () => {
        await newTester().test(
            "ReplaceCallWithStmt.js",
            "ReplaceCallWithStmt.c"
        );
    });

    it("InsertsLiteral", async () => {
        await newTester().test("InsertsLiteral.js", "inserts.c");
    });

    it("InsertsJp", async () => {
        await newTester().test("InsertsJp.js", "inserts.c");
    });

    it("Clone", async () => {
        await newTester().test("Clone.js", "clone.c");
    });

    it("AddGlobal", async () => {
        await newTester().test(
            "AddGlobal.js",
            "add_global_1.c",
            "add_global_2.c"
        );
    });

    it("Expressions", async () => {
        await newTester().test("Expressions.js", "expressions.c");
    });

    // FIXME: currentTime() is a method only available in the MasterWeaver class and not in any Weaver that is actually shipped.
    // This test only runs in the java test runner and does not work in the real world.
    /*
    it("Dijkstra", async () => {
        await newTester()
            .setCheckWovenCodeSyntax(false)
            .checkExpectedOutput(false)
            .test("Dijkstra.js", "dijkstra.c");
    });
    */

    it("Wrap", async () => {
        await newTester()
            .set(ClavaJavaTypes.CxxWeaverOption.PARSE_INCLUDES)
            .test("Wrap.js", "wrap.c", "wrap.h");
    });

    it("VarrefInWhile", async () => {
        await newTester().test("VarrefInWhile.js", "varref_in_while.c");
    });

    it("Inline", async () => {
        await newTester().test(
            "Inline.js",
            "inline.c",
            "inline_utils.h",
            "inline_utils.c"
        );
    });

    it("SetType", async () => {
        await newTester().test("SetType.js", "set_type.c");
    });

    it("Detach", async () => {
        await newTester().test("Detach.js", "detach.c");
    });

    (isWindows ? it.skip : it)("InlineNasLu", async () => {
        await newTester()
            .checkExpectedOutput(false)
            .test("InlineNasLu.js", "inline_nas_lu.c");
    });

    it("InlineNasFt", async () => {
        await newTester()
            .checkExpectedOutput(false)
            .test("InlineNasFt.js", "inline_nas_ft.c");
    });

    it("NullNodes", async () => {
        await newTester().test("NullNodes.js", "null_nodes.c");
    });

    it("TypeRenamer", async () => {
        await newTester().test("TypeRenamer.js", "type_renamer.c");
    });

    it("AstNodes", async () => {
        await newTester().test("AstNodes.js", "ast_nodes.c");
    });

    it("RemoveInclude", async () => {
        await newTester().test(
            "RemoveInclude.js",
            "remove_include.c",
            "remove_include_0.h",
            "remove_include_1.h",
            "remove_include_2.h"
        );
    });

    it("IncludeLocation", async () => {
        await newTester().test(
            "IncludeLoc.js",
            "remove_include.c",
            "remove_include_0.h",
            "remove_include_1.h",
            "remove_include_2.h"
        );
    });

    it("DynamicCallGraph", async () => {
        await newTester().test("DynamicCallGraph.js", "dynamic_call_graph.c");
    });

    it("Selects", async () => {
        await newTester().test("Selects.js", "selects.c");
    });

    it("Scope", async () => {
        await newTester().test("Scope.js", "scope.c");
    });

    it("Array", async () => {
        await newTester().test("ArrayTest.js", "array_test.c");
    });

    // @Test
    // it("OpenCLType", async () => {
    //     // TODO: Certain attributes are not supported yet (e.g., ReqdWorkGroupSizeAttr, WorkGroupSizeHintAttr,
    //     // VecTypeHintAttr)
    //     await newTester().set(ClavaOptions.STANDARD, Standard.OPENCL20).test("OpenCLType.js", "opencl_type.cl");
    // });

    it("Cilk", async () => {
        // Generated code has Cilk directives
        await newTester()
            .set(ClavaJavaTypes.ClavaOptions.FLAGS, "-fcilkplus")
            .test("Cilk.js", "cilk.c");
    });

    it("TagDecl", async () => {
        await newTester().test("TagDecl.js", "tag_decl.c");
    });

    it("File", async () => {
        await newTester().test("File.js", "file.c");
    });

    it("Switch", async () => {
        await newTester().test("SwitchTest.js", "switch.c");
    });

    it("AddParam", async () => {
        await newTester().test("AddParamTest.js", "add_param.c");
    });

    it("AddArg", async () => {
        await newTester().test("AddArgTest.js", "add_arg.c");
    });

    it("Cfg", async () => {
        await newTester().test("Cfg.js", "cfg.c");
    });

    it("ExprStmt", async () => {
        await newTester().test("ExprStmt.js", "expr_stmt.c");
    });

    it("Traversal", async () => {
        await newTester().test("Traversal.js", "traversal.c");
    });

    it("If", async () => {
        await newTester().test("If.js", "if.c");
    });
});

describe("CBenchTest", () => {
    function newTester() {
        return new ClavaLegacyTester(
            path.resolve("../ClavaWeaver/resources/clava/test/bench/"),
            ClavaJavaTypes.Standard.C99
        )
            .setResultPackage("c/results")
            .setSrcPackage("c/src");
    }

    it("HamidRegion", async () => {
        await newTester()
            .set(ClavaJavaTypes.CxxWeaverOption.PARSE_INCLUDES)
            .test("HamidRegion.js", "hamid_region.c", "hamid_region.h");
    });

    it("DspMatmul", async () => {
        await newTester().test("DspMatmul.js", "dsp_matmul.c");
    });

    it("LSIssue1", async () => {
        await newTester().test("LSIssue1.js", "ls_issue1.c");
    });
});

describe("CApiTest", () => {
    function newTester() {
        return new ClavaLegacyTester(
            path.resolve("../ClavaWeaver/resources/clava/test/api/"),
            ClavaJavaTypes.Standard.C99
        )
            .setSrcPackage("c/src/")
            .setResultPackage("c/results/");
    }

    it("Logger", async () => {
        await newTester().test("LoggerTest.js", "logger_test.c");
    });

    it("Timer", async () => {
        const tester = newTester();
        if (JavaTypes.SpecsPlatforms.isUnix()) {
            tester.setResultsFile("TimerTest.js.unix.txt");
        }

        await tester.test("TimerTest.js", "timer_test.c");
    });

    // Compiles C code, but with C++ flag.
    it("TimerWithCxxFlag", async () => {
        const tester = newTester();
        if (JavaTypes.SpecsPlatforms.isUnix()) {
            // Test not working on Unix
            return;
            // tester.set(ClavaOptions.STANDARD, Standard.C11).setResultsFile("TimerTest.js.unix.txt");
        }
        await tester
            .set(
                ClavaJavaTypes.ClavaOptions.STANDARD,
                ClavaJavaTypes.Standard.CXX11
            )
            .test("TimerTest.js", "timer_test.c");
    });

    it("Energy", async () => {
        // Disable syntax check of woven code, rapl include is not available
        await newTester()
            .setCheckWovenCodeSyntax(false)
            .test("EnergyTest.js", "energy_test.c");
    });

    it("CodeInserter", async () => {
        await newTester().test("CodeInserterTest.js", "code_inserter.c");
    });

    it("ArrayLinearizer", async () => {
        await newTester().test("ArrayLinearizerTest.js", "qr.c");
    });

    it("Selector", async () => {
        await newTester().test("SelectorTest.js", "selector_test.c");
    });

    it("StrcpyChecker", async () => {
        await newTester().test("StrcpyChecker.js", "strcpy.c");
    });

    it("StaticCallGraph", async () => {
        await newTester().test("StaticCallGraphTest.js", "static_call_graph.c");
    });

    it("PassComposition", async () => {
        await newTester().test("PassCompositionTest.js", "pass_composition.c");
    });

    it("CfgApi", async () => {
        await newTester().test("CfgApi.js", "cfg_api.c");
    });

    it("Inliner", async () => {
        await newTester().test("InlinerTest.js", "inliner.c");
    });

    it("StatementDecomposer", async () => {
        await newTester().test(
            "StatementDecomposerTest.js",
            "stmt_decomposer.c"
        );
    });

    it("ToSingleFile", async () => {
        await newTester().test(
            "ToSingleFile.js",
            "to_single_file_1.c",
            "to_single_file_2.c"
        );
    });

    it("LivenessAnalysis", async () => {
        await newTester().test(
            "LivenessAnalysisTest.js",
            "liveness_analysis.c"
        );
    });

    it("SwitchToIf", async () => {
        await newTester().test(
            "SwitchToIfTransformationTest.js",
            "switch_to_if.c"
        );
    });
});
