/**
 *  Copyright 2020 SPeCS.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package pt.up.fe.specs.clava.hls;

import java.io.File;
import java.util.ArrayList;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.analysis.flow.data.DFGUtils;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNodeType;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowSubgraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowSubgraphMetrics;
import pt.up.fe.specs.clava.hls.heuristics.InlineHeuristic;
import pt.up.fe.specs.clava.hls.heuristics.PipelineHeuristic;
import pt.up.fe.specs.clava.hls.strategies.ArrayStreamDetector;
import pt.up.fe.specs.clava.hls.strategies.CodeRegionPipelining;
import pt.up.fe.specs.clava.hls.strategies.FunctionInlining;
import pt.up.fe.specs.clava.hls.strategies.LoadStores;
import pt.up.fe.specs.clava.hls.strategies.NestedLoopUnrolling;
import pt.up.fe.specs.util.SpecsIo;

public class ClavaHLS {
    private DataFlowGraph dfg;
    private File weavingFolder;
    private boolean verbose = false;
    private final String separator = new String(new char[75]).replace('\0', '-');

    public ClavaHLS(DataFlowGraph dfg, File weavingFolder) {
	this.dfg = dfg;
	this.weavingFolder = weavingFolder;
	preprocessDfg();
    }

    public void applyFunctionInlining(double B) {
	log(separator);
	log("detecting if function calls can be inlined");
	FunctionInlining inliner = new FunctionInlining(dfg, B);
	inliner.analyze();
	inliner.apply();

	log(separator);
    }

    public void applyArrayStreaming() {
	log(separator);
	log("detecting if arrays can be turned into streams");
	ArrayStreamDetector arrayStream = new ArrayStreamDetector(dfg);
	arrayStream.analyze();
	if (arrayStream.detectedCases() > 0) {
	    log("found " + arrayStream.detectedCases() + " parameter(s) that can be declared as stream");
	    arrayStream.apply();
	}
	log(separator);
    }

    public void applyLoopStrategies(int p) {
	log(separator);
	log("defining unrolling factor for nested loops");
	NestedLoopUnrolling loopUnfolding = new NestedLoopUnrolling(dfg);
	loopUnfolding.analyze();
	loopUnfolding.apply();

	log("detecting if code regions can be pipelined");
	CodeRegionPipelining pipelining = new CodeRegionPipelining(dfg, loopUnfolding.getLoopsToUnroll(), p);
	pipelining.analyze();
	pipelining.apply();
	log(separator);
    }

    public void applyLoadStoresStrategy(int N) {
	log(separator);
	log("applying load/stores strategy only");
	LoadStores ls = new LoadStores(dfg, N);
	ls.analyze();
	if (ls.isSimpleLoop()) {
	    log("function is a simple loop, applying directives");
	    log("chosen Load/Stores factor is " + ls.getLsFactor());
	    ls.apply();
	} else {
	    log("function is not a simple loop, Load/Stores strategy cannot be applied");
	}
	log(separator);
    }

    public void applyGenericStrategies(ClavaHLSOptions options) {
	log(separator);
	if (dfg.hasConditionals()) {
	    this.verbose = true;
	    printDfg();
	    return;
	}
	log("applying HLS optimization with all generic strategies");
	printDfg();
	printSubgraphCosts();

	this.applyArrayStreaming();
	this.applyFunctionInlining(InlineHeuristic.DEFAULT_B);
	this.applyLoopStrategies(PipelineHeuristic.DEFAULT_FACTOR);

	log("finished HLS optimization");
	log(separator);
    }

    public boolean canBeInstrumented() {
	log("checking if function can be turned into a trace");
	TracingValidator valid = new TracingValidator(dfg);
	boolean able = valid.validate();
	if (able)
	    log("function can be turned into a trace!");
	else
	    log("function cannot be turned into a trace");
	return able;
    }

    public static void log(String msg) {
	ClavaLog.info("HLS: " + msg);
    }

    private void printDfg() {
	StringBuilder sb = new StringBuilder();
	sb.append(dfg.getFunctionName()).append(".dot");
	String dot = dfg.toDot();
	saveFile(weavingFolder, "graphs", sb.toString(), dfg.toDot());

	if (verbose) {
	    log("using the following DFG as input:");
	    log("----------------------------------");
	    ClavaHLS.log("\n" + dot);
	    log("----------------------------------");
	}
    }

    private void printSubgraphCosts() {
	log("reporting the cost of each subgraph");
	StringBuilder sb = new StringBuilder();
	String NL = "\n";
	sb.append(DataFlowSubgraphMetrics.HEADER).append(NL);
	for (DataFlowNode node : dfg.getSubgraphRoots()) {
	    DataFlowSubgraph sub = dfg.getSubgraph(node);
	    DataFlowSubgraphMetrics m = sub.getMetrics();
	    m.setIterations(DFGUtils.estimateNodeFrequency(sub.getRoot()));
	    sb.append(m.toString()).append(NL);
	}
	StringBuilder fileName = new StringBuilder();
	fileName.append("features_").append(dfg.getFunctionName()).append(".csv");
	saveFile(weavingFolder, "reports", fileName.toString(), sb.toString());
    }

    private void preprocessDfg() {
	for (DataFlowNode node : dfg.getSubgraphRoots()) {
	    // Identify indexes
	    DataFlowSubgraph sub = dfg.getSubgraph(node);
	    ArrayList<DataFlowNode> loads = DFGUtils.getVarLoadsOfSubgraph(sub);
	    for (DataFlowNode load : loads) {
		if (DFGUtils.isIndex(load))
		    load.setType(DataFlowNodeType.LOAD_INDEX);
	    }
	    // Merge loads and stores of the same var
	    String storeLabel = sub.getRoot().getLabel();
	    ArrayList<DataFlowNode> nodesToMerge = new ArrayList<>();
	    nodesToMerge.add(sub.getRoot());
	    for (DataFlowNode n : sub.getNodes()) {
		if (n.getLabel().contentEquals(storeLabel)) {
		    nodesToMerge.add(n);
		}
	    }
	    dfg.mergeNodes(nodesToMerge);
	}
    }

    public void saveFile(File weavingFolder, String reportType, String fileName, String fileContents) {
	SpecsIo.mkdir(weavingFolder);
	StringBuilder path = new StringBuilder();
	path.append(weavingFolder.getPath().toString()).append(File.separator).append("_HLS_").append(reportType)
		.append(File.separator).append(fileName);
	if (SpecsIo.write(new File(path.toString()), fileContents))
	    ClavaHLS.log("file \"" + fileName + "\" saved to \"" + path.toString() + "\"");
	else
	    ClavaHLS.log("failed to save file \"" + fileName + "\"");
    }

    @Deprecated
    public void run(ClavaHLSOptions mode) {
	if (mode.decide) {
	    if (canBeInstrumented()) {
		mode.trace = true;
	    } else {
		mode.directives = true;
	    }
	}
	mode.directives = true; // JUST FOR DEBUG
	if (mode.trace) {
	    // Call LARA?
	}
	if (mode.directives) {
	    applyGenericStrategies(mode);
	}
    }
}
