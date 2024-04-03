
/**
 * Copyright 2024 SPeCS.
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

package pt.up.fe.specs.clava.weaver;

import org.lara.interpreter.weaver.utils.LaraResourceProvider;

/**
 * This file has been automatically generated.
 * 
 * @author Joao Bispo, Luis Sousa
 *
 */
public enum ClavaApiJsResource implements LaraResourceProvider {

    JOINPOINTS_JS("Joinpoints.js"),
    CLAVA_JS("clava/Clava.js"),
    CLAVACODE_JS("clava/ClavaCode.js"),
    CLAVAJAVATYPES_JS("clava/ClavaJavaTypes.js"),
    CLAVAJOINPOINTS_JS("clava/ClavaJoinPoints.js"),
    CLAVATYPE_JS("clava/ClavaType.js"),
    FORMAT_JS("clava/Format.js"),
    MATHEXTRA_JS("clava/MathExtra.js"),
    ANALYSER_JS("clava/analysis/Analyser.js"),
    ANALYSERRESULT_JS("clava/analysis/AnalyserResult.js"),
    CHECKBASEDANALYSER_JS("clava/analysis/CheckBasedAnalyser.js"),
    CHECKRESULT_JS("clava/analysis/CheckResult.js"),
    CHECKER_JS("clava/analysis/Checker.js"),
    FIX_JS("clava/analysis/Fix.js"),
    MESSAGEGENERATOR_JS("clava/analysis/MessageGenerator.js"),
    RESULTFORMATMANAGER_JS("clava/analysis/ResultFormatManager.js"),
    RESULTLIST_JS("clava/analysis/ResultList.js"),
    BOUNDSANALYSER_JS("clava/analysis/analysers/BoundsAnalyser.js"),
    BOUNDSRESULT_JS("clava/analysis/analysers/BoundsResult.js"),
    DOUBLEFREEANALYSER_JS("clava/analysis/analysers/DoubleFreeAnalyser.js"),
    DOUBLEFREERESULT_JS("clava/analysis/analysers/DoubleFreeResult.js"),
    CHGRPCHECKER_JS("clava/analysis/checkers/ChgrpChecker.js"),
    CHMODCHECKER_JS("clava/analysis/checkers/ChmodChecker.js"),
    CHOWNCHECKER_JS("clava/analysis/checkers/ChownChecker.js"),
    CINCHECKER_JS("clava/analysis/checkers/CinChecker.js"),
    EXECCHECKER_JS("clava/analysis/checkers/ExecChecker.js"),
    FPRINTFCHECKER_JS("clava/analysis/checkers/FprintfChecker.js"),
    FSCANFCHECKER_JS("clava/analysis/checkers/FscanfChecker.js"),
    GETSCHECKER_JS("clava/analysis/checkers/GetsChecker.js"),
    LAMBDACHECKER_JS("clava/analysis/checkers/LambdaChecker.js"),
    MEMCPYCHECKER_JS("clava/analysis/checkers/MemcpyChecker.js"),
    PRINTFCHECKER_JS("clava/analysis/checkers/PrintfChecker.js"),
    SCANFCHECKER_JS("clava/analysis/checkers/ScanfChecker.js"),
    SPRINTFCHECKER_JS("clava/analysis/checkers/SprintfChecker.js"),
    STRCATCHECKER_JS("clava/analysis/checkers/StrcatChecker.js"),
    STRCPYCHECKER_JS("clava/analysis/checkers/StrcpyChecker.js"),
    SYSLOGCHECKER_JS("clava/analysis/checkers/SyslogChecker.js"),
    SYSTEMCHECKER_JS("clava/analysis/checkers/SystemChecker.js"),
    CMAKER_JS("clava/cmake/CMaker.js"),
    CMAKERSOURCES_JS("clava/cmake/CMakerSources.js"),
    CMAKERUTILS_JS("clava/cmake/CMakerUtils.js"),
    CMAKECOMPILER_JS("clava/cmake/compilers/CMakeCompiler.js"),
    GENERICCMAKECOMPILER_JS("clava/cmake/compilers/GenericCMakeCompiler.js"),
    DECOMPOSERESULT_JS("clava/code/DecomposeResult.js"),
    DOTOWHILESTMT_JS("clava/code/DoToWhileStmt.js"),
    FORTOWHILESTMT_JS("clava/code/ForToWhileStmt.js"),
    GLOBALVARIABLE_JS("clava/code/GlobalVariable.js"),
    INLINER_JS("clava/code/Inliner.js"),
    OUTLINER_JS("clava/code/Outliner.js"),
    REMOVESHADOWING_JS("clava/code/RemoveShadowing.js"),
    SIMPLIFYASSIGNMENT_JS("clava/code/SimplifyAssignment.js"),
    SIMPLIFYTERNARYOP_JS("clava/code/SimplifyTernaryOp.js"),
    STATEMENTDECOMPOSER_JS("clava/code/StatementDecomposer.js"),
    GPROFER_JS("clava/gprofer/Gprofer.js"),
    CONTROLFLOWGRAPH_JS("clava/graphs/ControlFlowGraph.js"),
    STATICCALLGRAPH_JS("clava/graphs/StaticCallGraph.js"),
    CFGBUILDER_JS("clava/graphs/cfg/CfgBuilder.js"),
    CFGEDGE_JS("clava/graphs/cfg/CfgEdge.js"),
    CFGEDGETYPE_JS("clava/graphs/cfg/CfgEdgeType.js"),
    CFGNODEDATA_JS("clava/graphs/cfg/CfgNodeData.js"),
    CFGNODETYPE_JS("clava/graphs/cfg/CfgNodeType.js"),
    CFGUTILS_JS("clava/graphs/cfg/CfgUtils.js"),
    NEXTCFGNODE_JS("clava/graphs/cfg/NextCfgNode.js"),
    CASEDATA_JS("clava/graphs/cfg/nodedata/CaseData.js"),
    DATAFACTORY_JS("clava/graphs/cfg/nodedata/DataFactory.js"),
    GOTODATA_JS("clava/graphs/cfg/nodedata/GotoData.js"),
    HEADERDATA_JS("clava/graphs/cfg/nodedata/HeaderData.js"),
    IFDATA_JS("clava/graphs/cfg/nodedata/IfData.js"),
    INSTLISTNODEDATA_JS("clava/graphs/cfg/nodedata/InstListNodeData.js"),
    LABELDATA_JS("clava/graphs/cfg/nodedata/LabelData.js"),
    LOOPDATA_JS("clava/graphs/cfg/nodedata/LoopData.js"),
    RETURNDATA_JS("clava/graphs/cfg/nodedata/ReturnData.js"),
    SCOPENODEDATA_JS("clava/graphs/cfg/nodedata/ScopeNodeData.js"),
    SWITCHDATA_JS("clava/graphs/cfg/nodedata/SwitchData.js"),
    SCGEDGEDATA_JS("clava/graphs/scg/ScgEdgeData.js"),
    SCGNODEDATA_JS("clava/graphs/scg/ScgNodeData.js"),
    STATICCALLGRAPHBUILDER_JS("clava/graphs/scg/StaticCallGraphBuilder.js"),
    HDF5_JS("clava/hdf5/Hdf5.js"),
    HLSANALYSIS_JS("clava/hls/HLSAnalysis.js"),
    MATHANALYSIS_JS("clava/hls/MathAnalysis.js"),
    MATHHINFO_JS("clava/hls/MathHInfo.js"),
    TRACEINSTRUMENTATION_JS("clava/hls/TraceInstrumentation.js"),
    LIVENESSANALYSER_JS("clava/liveness/LivenessAnalyser.js"),
    LIVENESSANALYSIS_JS("clava/liveness/LivenessAnalysis.js"),
    LIVENESSUTILS_JS("clava/liveness/LivenessUtils.js"),
    MEMOIANALYSIS_JS("clava/memoi/MemoiAnalysis.js"),
    MEMOIGEN_JS("clava/memoi/MemoiGen.js"),
    MEMOIPROF_JS("clava/memoi/MemoiProf.js"),
    MEMOITARGET_JS("clava/memoi/MemoiTarget.js"),
    MEMOIUTILS_JS("clava/memoi/MemoiUtils.js"),
    _MEMOIGENHELPER_JS("clava/memoi/_MemoiGenHelper.js"),
    MPIACCESSPATTERN_JS("clava/mpi/MpiAccessPattern.js"),
    MPISCATTERGATHERLOOP_JS("clava/mpi/MpiScatterGatherLoop.js"),
    MPIUTILS_JS("clava/mpi/MpiUtils.js"),
    ITERATIONVARIABLEPATTERN_JS("clava/mpi/patterns/IterationVariablePattern.js"),
    MPIACCESSPATTERNS_JS("clava/mpi/patterns/MpiAccessPatterns.js"),
    SCALARPATTERN_JS("clava/mpi/patterns/ScalarPattern.js"),
    KERNELREPLACER_JS("clava/opencl/KernelReplacer.js"),
    KERNELREPLACERAUTO_JS("clava/opencl/KernelReplacerAuto.js"),
    OPENCLCALL_JS("clava/opencl/OpenCLCall.js"),
    OPENCLCALLVARIABLES_JS("clava/opencl/OpenCLCallVariables.js"),
    INLINING_JS("clava/opt/Inlining.js"),
    NORMALIZETOSUBSET_JS("clava/opt/NormalizeToSubset.js"),
    PREPAREFORINLINING_JS("clava/opt/PrepareForInlining.js"),
    BATCHPARSER_JS("clava/parser/BatchParser.js"),
    DECOMPOSEDECLSTMT_JS("clava/pass/DecomposeDeclStmt.js"),
    DECOMPOSEVARDECLARATIONS_JS("clava/pass/DecomposeVarDeclarations.js"),
    LOCALSTATICTOGLOBAL_JS("clava/pass/LocalStaticToGlobal.js"),
    SIMPLIFYLOOPS_JS("clava/pass/SimplifyLoops.js"),
    SIMPLIFYRETURNSTMTS_JS("clava/pass/SimplifyReturnStmts.js"),
    SIMPLIFYSELECTIONSTMTS_JS("clava/pass/SimplifySelectionStmts.js"),
    SINGLERETURNFUNCTION_JS("clava/pass/SingleReturnFunction.js"),
    TRANSFORMSWITCHTOIF_JS("clava/pass/TransformSwitchToIf.js"),
    OPSBLOCK_JS("clava/stats/OpsBlock.js"),
    OPSCOST_JS("clava/stats/OpsCost.js"),
    OPSCOUNTER_JS("clava/stats/OpsCounter.js"),
    STATICOPSCOUNTER_JS("clava/stats/StaticOpsCounter.js"),
    CLAVADATASTORE_JS("clava/util/ClavaDataStore.js"),
    CODEINSERTER_JS("clava/util/CodeInserter.js"),
    FILEITERATOR_JS("clava/util/FileIterator.js"),
    DETECTSTREAM_JS("clava/uve/DetectStream.js"),
    UVE_JS("clava/uve/UVE.js"),
    VITISHLS_JS("clava/vitishls/VitisHls.js"),
    VITISHLSREPORTPARSER_JS("clava/vitishls/VitisHlsReportParser.js"),
    VITISHLSUTILS_JS("clava/vitishls/VitisHlsUtils.js"),
    CORE_JS("core.js"),
    CLAVABENCHMARKINSTANCE_JS("lara/benchmark/ClavaBenchmarkInstance.js"),
    ENERGY_JS("lara/code/Energy.js"),
    LOGGER_JS("lara/code/Logger.js"),
    TIMER_JS("lara/code/Timer.js"),
    ENERGYMETRIC_JS("lara/metrics/EnergyMetric.js"),
    EXECUTIONTIMEMETRIC_JS("lara/metrics/ExecutionTimeMetric.js"),
    WEAVERLAUNCHER_JS("weaver/WeaverLauncher.js");

    private final String resource;

    private static final String WEAVER_PACKAGE = "clava/";

    /**
     * @param resource
     */
    private ClavaApiJsResource (String resource) {
      this.resource = WEAVER_PACKAGE + getSeparatorChar() + resource;
    }

    /* (non-Javadoc)
     * @see org.suikasoft.SharedLibrary.Interfaces.ResourceProvider#getResource()
     */
    @Override
    public String getOriginalResource() {
        return resource;
    }

}
