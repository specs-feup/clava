import { ClavaWeaverTester } from "./LegacyIntegrationTestsHelpers.test.js";
import JavaTypes from "@specs-feup/lara/api/lara/util/JavaTypes.js";
import ClavaJavaTypes from "@specs-feup/clava/api/clava/ClavaJavaTypes.js";
import path from "path";

/* eslint-disable jest/expect-expect */
describe("CxxTest", () => {
    function newTester() {
        return new ClavaWeaverTester(
            path.resolve("../ClavaWeaver/resources/clava/test/weaver"),
            ClavaJavaTypes.Standard.CXX11
        )
            .setResultPackage("cpp/results")
            .setSrcPackage("cpp/src");
    }

    it("Statement", async () => {
        await newTester().test("Statement.js", "statement.cpp");
    });

    it("Loop", async () => {
        await newTester().test("Loop.js", "loop.cpp");
    });

    it("ReplaceCallWithStmt", async () => {
        await newTester().test(
            "ReplaceCallWithStmt.js",
            "ReplaceCallWithStmt.cpp"
        );
    });

    it("InsertsLiteral", async () => {
        await newTester().test("InsertsLiteral.js", "inserts.cpp");
    });

    it("InsertsJp", async () => {
        await newTester().test("InsertsJp.js", "inserts.cpp");
    });

    it("Pragmas", async () => {
        await newTester().test("Pragmas.js", "pragma.cpp");
    });

    it("Pragmas2", async () => {
        await newTester().test("Pragma2.js", "pragma2.cpp");
    });

    it("Actions", async () => {
        await newTester().test("Actions.js", "actions.cpp");
    });

    it("ArrayAccess", async () => {
        await newTester().test(
            "ArrayAccess.js",
            "array_access.cpp",
            "array_access.h"
        );
    });

    it("AttributeUse", async () => {
        await newTester().test("AttributeUse.js", "attribute_use.cpp");
    });

    it("Hdf5Types", async () => {
        await newTester()
            // Disable syntax checking, since test system may not have HDF5 includes automatically available
            .setCheckWovenCodeSyntax(false)
            .set(ClavaJavaTypes.CxxWeaverOption.PARSE_INCLUDES)
            .test("Hdf5Types.js", "hdf5types.cpp");
    });

    it("OmpThreadsExplore", async () => {
        await newTester().test(
            "OmpThreadsExplore.js",
            "omp_threads_explore.cpp"
        );
    });

    it("HamidCfg", async () => {
        await newTester().test("HamidCfg.js", "dijkstra.cpp");
    });

    it("Clone", async () => {
        await newTester()
            .set(ClavaJavaTypes.CxxWeaverOption.PARSE_INCLUDES)
            .test("Clone.js", "clone.cpp", "clone.h");
    });

    it("AddGlobal", async () => {
        await newTester().test(
            "AddGlobal.js",
            "add_global_1.cpp",
            "add_global_2.cpp"
        );
    });

    it("Omp", async () => {
        await newTester().test("Omp.js", "omp.cpp");
    });

    it("OmpAttributes", async () => {
        await newTester().test("OmpAttributes.js", "omp_attributes.cpp");
    });

    it("OmpSetAttributes", async () => {
        await newTester().test("OmpSetAttributes.js", "omp_set_attributes.cpp");
    });

    it("Expressions", async () => {
        await newTester().test("Expressions.js", "expressions.cpp", "classA.h");
    });

    it("ParentRegion", async () => {
        await newTester().test("ParentRegion.js", "parent_region.cpp");
    });

    it("VarDecl", async () => {
        await newTester().test("Vardecl.js", "vardecl.cpp");
    });

    it("ParamType", async () => {
        await newTester()
            .checkExpectedOutput(false)
            .test("ParamType.js", "param_type.cpp");
    });

    it("Wrap", async () => {
        if (JavaTypes.SpecsSystem.isWindows()) {
            console.info("Skipping test, results are different on Windows");
            return;
        }

        // newTester().test("Wrap.js", "wrap.cpp", "wrap.h", "lib/lib.h", "lib/lib.cpp");
        await newTester()
            .set(ClavaJavaTypes.CxxWeaverOption.PARSE_INCLUDES)
            .test("Wrap.js", "wrap.cpp", "wrap.h");
    });

    it("SelectVarDecl", async () => {
        await newTester().test("SelectVardecl.js", "select_vardecl.cpp");
    });

    it("Macros", async () => {
        await newTester()
            .setCheckWovenCodeSyntax(false)
            .test("Macros.js", "macros.cpp");
    });

    it("Call", async () => {
        await newTester().test("Call.js", "call.cpp");
    });

    it("PragmaClavaAttribute", async () => {
        await newTester().test("PragmaAttribute.js", "pragma_attribute.cpp");
    });

    it("TypeTemplate", async () => {
        await newTester().test("TypeTemplate.js", "type_template.cpp");
    });

    it("Function", async () => {
        await newTester().test("Function.js", "function.cpp", "function.h");
    });

    it("AstAttributes", async () => {
        await newTester().test("AstAttributes.js", "ast_attributes.cpp");
    });

    it("PragmaData", async () => {
        await newTester().test(
            "PragmaData.js",
            "pragma_data.cpp",
            "pragma_data_2.cpp"
        );
    });

    it("GlobalAttributes", async () => {
        await newTester().test("GlobalAttributes.js", "global_attributes.cpp");
    });

    it("SetType", async () => {
        await newTester().test("SetTypeCxx.js", "set_type.cpp");
    });

    it("MultiFile", async () => {
        await newTester()
            .set(ClavaJavaTypes.CxxWeaverOption.PARSE_INCLUDES)
            .test("MultiFile.js", "multiFile.cpp", "multiFile.h");
    });

    it("Field", async () => {
        await newTester().test("Field.js", "field.hpp");
    });

    it("FileRebuild", async () => {
        await newTester()
            .set(ClavaJavaTypes.CxxWeaverOption.PARSE_INCLUDES)
            .test(
                "FileRebuild.js",
                "file_rebuild.cpp",
                "file_rebuild.h",
                "file_rebuild_2.h"
            );
    });

    it("Setters", async () => {
        await newTester().test("Setters.js", "setters.cpp");
    });

    it("SkipParsingHeaders", async () => {
        await newTester().test(
            "SkipParsingHeaders.js",
            "skip_parsing_headers.cpp",
            "skip_parsing_headers.h"
        );
    });

    it("NoParsing", async () => {
        await newTester().test("NoParsing.js");
    });

    it("LaraGetter", async () => {
        await newTester().test("LaraGetter.js");
    });

    it("VarDeclV2", async () => {
        await newTester().test(
            "VardeclV2.js",
            "vardeclv2.cpp",
            "vardeclv2_2.cpp"
        );
    });

    it("File", async () => {
        await newTester().test("File.js", "file.cpp");
    });

    it("DataClass", async () => {
        await newTester().test("DataClass.js", "dataclass.cpp");
    });

    it("ClassManipulation", async () => {
        await newTester()
            .set(ClavaJavaTypes.CxxWeaverOption.PARSE_INCLUDES)
            .test(
                "ClassManipulation.js",
                "class_manipulation.cpp",
                "class_manipulation.h"
            );
    });

    it("This", async () => {
        await newTester().test("ThisTest.js", "this.cpp");
    });

    it("Member", async () => {
        await newTester().test("Member.js", "member.cpp");
    });

    it("FieldRef", async () => {
        await newTester()
            .checkExpectedOutput(false)
            .test("FieldRef.js", "fieldRef.cpp");
    });

    it("ExpressionDecls", async () => {
        await newTester().test("ExpressionDecls.js", "expressionDecls.cpp");
    });

    it("TemplateSpecializationType", async () => {
        await newTester().test(
            "TemplateSpecializationType.js",
            "template_specialization_type.cpp"
        );
    });

    it("ReverseIterator", async () => {
        await newTester().test("ReverseIterator.js", "reverse_iterator.cpp");
    });

    it("Function2", async () => {
        await newTester().test("Function2.js", "function2.cpp");
    });

    it("CloneOnFile", async () => {
        await newTester()
            // Generates a file in another folder, needs to generate the header file otherwise it will not parse
            // correctly the second time
            .set(ClavaJavaTypes.CxxWeaverOption.PARSE_INCLUDES)
            .test("CloneOnFile.js", "clone_on_file.cpp", "clone_on_file.h");
    });

    it("EmptyStmt", async () => {
        await newTester().test("EmptyStmt.js", "empty_stmt.cpp");
    });

    it("Class", async () => {
        await newTester().test("Class.js", "class.cpp");
    });

    it("Canonical", async () => {
        await newTester().test("CanonicalTest.js", "canonical.cpp");
    });

    it("Break", async () => {
        await newTester().test("Break.js", "break.cpp");
    });
});

describe("CxxBenchTest", () => {
    function newTester() {
        return new ClavaWeaverTester(
            path.resolve("../ClavaWeaver/resources/clava/test/bench/"),
            ClavaJavaTypes.Standard.CXX11
        )
            .setResultPackage("cpp/results")
            .setSrcPackage("cpp/src");
    }

    it("LoicEx1", async () => {
        await newTester().test("LoicEx1.js", "loic_ex1.cpp");
    });

    it("LoicEx2", async () => {
        await newTester()
            .setCheckWovenCodeSyntax(false)
            .checkExpectedOutput(false)
            .test("LoicEx2.js", "loic_ex2.cpp");
    });

    it("LoicEx3", async () => {
        await newTester().test("LoicEx3.js", "loic_ex3.cpp");
    });

    it("LSIssue2", async () => {
        await newTester().test("LSIssue2.js", "ls_issue2.cpp");
    });

    it("CbMultios", async () => {
        await newTester().test("CbMultios.js", "cb_multios.cpp");
    });
});

describe("CxxApiTest", () => {
    function newTester() {
        return new ClavaWeaverTester(
            path.resolve("../ClavaWeaver/resources/clava/test/api/"),
            ClavaJavaTypes.Standard.CXX11
        )
            .setSrcPackage("cpp/src/")
            .setResultPackage("cpp/results/");
    }

    it("Logger", async () => {
        await newTester().test("LoggerTest.js", "logger_test.cpp");
    });

    it("LoggerWithLibrary", async () => {
        // Disable syntax check of woven code, SpecsLogger includes are not available
        await newTester()
            .setCheckWovenCodeSyntax(false)
            .test("LoggerTestWithLib.js", "logger_test.cpp");
    });

    it("Energy", async () => {
        // Disable syntax check of woven code, rapl include is not available
        await newTester()
            .setCheckWovenCodeSyntax(false)
            .test("EnergyTest.js", "energy_test.cpp");
    });

    it("Timer", async () => {
        await newTester().test("TimerTest.js", "timer_test.cpp");
    });

    it("ClavaFindJp", async () => {
        await newTester().test("ClavaFindJpTest.js", "clava_find_jptest.cpp");
    });

    // TODO: Figure out a way to check if CMake is available
    /*
    it("CMaker", async () => {
        if (!IS_CMAKE_AVAILABLE.get()) {
            return;
        }

        await newTester()
                .set(ClavaJavaTypes.CxxWeaverOption.PARSE_INCLUDES)
                .test("CMakerTest.js", "cmaker_test.cpp", "cmaker_test.h");
    });
    */

    it("MathExtra", async () => {
        await newTester().test("MathExtraTest.js", "math_extra_test.cpp");
    });

    it("WeaverLauncher", async () => {
        await newTester().test(
            "WeaverLauncherTest.js",
            "weaver_launcher_test.cpp"
        );
    });

    it("ClavaDataStore", async () => {
        await newTester().test(
            "ClavaDataStoreTest.js",
            "clava_data_store_test.cpp"
        );
    });

    it("UserValues", async () => {
        await newTester().test("UserValuesTest.js", "user_values.cpp");
    });

    it("ClavaCode", async () => {
        await newTester().test("ClavaCodeTest.js", "clava_code.cpp");
    });

    it("ClavaJoinPointsTest", async () => {
        await newTester().test(
            "ClavaJoinPointsTest.js",
            "clava_join_points.cpp"
        );
    });

    it("JpFilter", async () => {
        await newTester()
            .set(ClavaJavaTypes.CxxWeaverOption.PARSE_INCLUDES)
            .test("JpFilter.js", "jp_filter.hpp");
    });

    it("Rebuild", async () => {
        await newTester().test("RebuildTest.js", "rebuild.cpp");
    });

    it("FileIterator", async () => {
        await newTester().test(
            "FileIteratorTest.js",
            "file_iterator_1.cpp",
            "file_iterator_2.cpp"
        );
    });

    it("AddHeaderFile", async () => {
        await newTester().test("AddHeaderFileTest.js", "add_header_file.h");
    });

    it("Clava", async () => {
        await newTester().test("ClavaTest.js", "clava.cpp");
    });

    it("Query", async () => {
        await newTester().test("QueryTest.js", "query.cpp");
    });

    it("ClavaType", async () => {
        await newTester().test("ClavaTypeTest.js");
    });

    it("StatementDecomposer", async () => {
        await newTester().test(
            "StatementDecomposerTest.js",
            "stmt_decomposer.cpp"
        );
    });

    it("SimplifyVarDeclarations", async () => {
        await newTester().test(
            "PassSimplifyVarDeclarations.js",
            "pass_simplify_var_declarations.cpp"
        );
    });

    it("SingleReturnFunction", async () => {
        await newTester().test(
            "PassSingleReturnTest.js",
            "pass_single_return.cpp"
        );
    });

    it("SimplifyAssignment", async () => {
        await newTester().test(
            "CodeSimplifyAssignmentTest.js",
            "code_simplify_assignment.cpp"
        );
    });

    it("SimplifyTernaryOp", async () => {
        await newTester().test(
            "CodeSimplifyTernaryOpTest.js",
            "code_simplify_ternary_op.cpp"
        );
    });

    it("SimplifyLoops", async () => {
        await newTester().test(
            "PassSimplifyLoopsTest.js",
            "pass_simplify_loops.cpp"
        );
    });

    it("MpiScatterGather", async () => {
        await newTester()
            // Disable syntax check of woven code, mpi.h may not be available
            .setCheckWovenCodeSyntax(false)
            .test(
                "MpiScatterGatherTest.js",
                "mpi_scatter_gather.cpp",
                "mpi_scatter_gather.h"
            );
    });

    it("Subset", async () => {
        await newTester().test("SubsetTest.js", "subset.cpp");
    });
});

/*
describe("CTest", () => {
    function newTester() {
        return new ClavaWeaverTester(
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

    it("Dijkstra", async () => {
        await newTester()
            .setCheckWovenCodeSyntax(false)
            .checkExpectedOutput(false)
            .test("Dijkstra.js", "dijkstra.c");
    });

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

    it("InlineNasLu", async () => {
        if (JavaTypes.SpecsSystem.isWindows()) {
            console.info("Skipping test, does not work on Windows");
            return;
        }

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

    // TODO: Implement a way to get ClavaOptions.
    // it("Cilk", async () => {
    //     // Generated code has Cilk directives
    //     await newTester()
    //         .set(ClavaJavaTypes.ClavaOptions.FLAGS, "-fcilkplus")
    //         .test("Cilk.js", "cilk.c");
    // });

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
        return new ClavaWeaverTester(
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
        return new ClavaWeaverTester(
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
    // TODO: Implement a way to get ClavaOptions.
    // it("TimerWithCxxFlag", async () => {
    //     const tester = newTester();
    //     if (JavaTypes.SpecsPlatforms.isUnix()) {
    //         // Test not working on Unix
    //         return;
    //         // tester.set(ClavaOptions.STANDARD, Standard.C11).setResultsFile("TimerTest.js.unix.txt");
    //     }
    //
    //     await tester.set(ClavaJavaTypes.ClavaOptions.STANDARD, ClavaJavaTypes.Standard.CXX11).test("TimerTest.js", "timer_test.c");
    // });

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

    it("Hls", async () => {
        await newTester().test("HlsTest.js", "hls.c");
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
*/

// TODO: Implement a way to get CodeParser.
/*
describe("CudaTest", () => {
    function newTester() {
        const cudaTester = new ClavaWeaverTester(
            path.resolve("../ClavaWeaver/resources/clava/test/weaver/),
            ClavaJavaTypes.Standard.CUDA
        )
            .setResultPackage("cuda/results")
            .setSrcPackage("cuda/src")
            .set(ClavaJavaTypes.CodeParser.CUDA_PATH, ClavaJavaTypes.CodeParser.getBuiltinOption());

        // Windows currently not supported
        if (JavaTypes.SpecsPlatforms.isWindows()) {
            cudaTester.doNotRun();
        }

        return cudaTester;
    }

    it("Cuda", async () => {
        await newTester().test("Cuda.js", "atomicAdd.cu");
    });

    it("CudaMatrixMul", async () => {
        await newTester().test("CudaMatrixMul.js", "mult_matrix.cu");
    });

    it("CudaQuery", async () => {
        await newTester().test("CudaQuery.js", "sample.cu");
    });
});
*/
