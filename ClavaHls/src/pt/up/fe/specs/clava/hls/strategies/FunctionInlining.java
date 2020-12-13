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

package pt.up.fe.specs.clava.hls.strategies;

import java.util.ArrayList;
import java.util.HashMap;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.analysis.flow.data.DFGUtils;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNodeType;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowSubgraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowSubgraphMetrics;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.hls.ClavaHLS;
import pt.up.fe.specs.clava.hls.directives.HLSInline;
import pt.up.fe.specs.clava.hls.directives.HLSInline.InlineType;
import pt.up.fe.specs.clava.hls.heuristics.InlineHeuristic;

public class FunctionInlining extends RestructuringStrategy {
    private HashMap<String, Long> callFreq;
    private HashMap<String, DataFlowGraph> functions;
    private HashMap<String, Boolean> isFunctionInlinable;
    private HashMap<String, Long> functionCosts;
    private HashMap<String, Boolean> canInline;
    private double B;

    public FunctionInlining(DataFlowGraph dfg, double B) {
	super(dfg);
	callFreq = new HashMap<>();
	functions = new HashMap<>();
	isFunctionInlinable = new HashMap<>();
	functionCosts = new HashMap<>();
	isFunctionInlinable = new HashMap<>();
	canInline = new HashMap<>();
	this.B = B;
    }

    @Override
    public void analyze() {
	ArrayList<DataFlowNode> calls = DFGUtils.getAllNodesOfType(dfg, DataFlowNodeType.OP_CALL);

	// Check if called functions are inlinable, and get their costs
	long mainFunCost = analyzeFunction(dfg);
	findDistinctFunctions(calls);

	// Remove calls to non-inlinable functions
	isFunctionInlinable.forEach((k, v) -> {
	    if (!v)
		calls.removeIf(n -> n.getLabel() == k);
	});

	// Check if each call is inlinable
	estimateCallFrequencies(calls);

	for (String funName : functions.keySet()) {
	    Long freq = callFreq.get(funName);
	    Long cost = functionCosts.get(funName);
	    boolean inline = InlineHeuristic.calculate(freq, cost, mainFunCost, B);
	    canInline.put(funName, inline);
	}
    }

    private void findDistinctFunctions(ArrayList<DataFlowNode> calls) {
	for (DataFlowNode call : calls) {
	    String funName = call.getLabel();
	    if (!functions.containsKey(funName)) {
		DataFlowGraph dfg = getCallDfg(call);
		if (dfg != null) {
		    functions.put(funName, dfg);
		    functionCosts.put(funName, analyzeFunction(funName));
		} else
		    isFunctionInlinable.put(call.getLabel(), false);
	    }
	}
    }

    private DataFlowGraph getCallDfg(DataFlowNode call) {
	CallExpr callNode = (CallExpr) call.getClavaNode();
	if (!callNode.getDefinition().isPresent()) {
	    ClavaHLS.log("function defininion of \"" + call.getLabel() + "\" not found (cannot be inlined)");
	    return null;
	}
	FunctionDecl fun = callNode.getDefinition().get();

	if (!fun.getBody().isPresent()) {
	    ClavaHLS.log("function body of \"" + call.getLabel() + "\" not found (cannot be inlined)");
	    return null;
	}
	CompoundStmt scope = fun.getBody().get();
	ClavaHLS.log("function body of \"" + call.getLabel() + "\" found (can be inlined)");
	DataFlowGraph funDFG = new DataFlowGraph(scope);
	return funDFG;
    }

    private void estimateCallFrequencies(ArrayList<DataFlowNode> calls) {
	for (DataFlowNode call : calls) {
	    String funName = call.getLabel();
	    long freq = DFGUtils.estimateNodeFrequency(call);
	    if (callFreq.containsKey(funName)) {
		if (callFreq.get(funName) < freq)
		    callFreq.put(funName, freq);
	    } else
		callFreq.put(funName, freq);
	}
    }

    public long analyzeFunction(String fun) {
	DataFlowGraph dfg = functions.get(fun);
	return analyzeFunction(dfg);
    }

    public long analyzeFunction(DataFlowGraph dfg) {
	ArrayList<DataFlowSubgraphMetrics> metrics = new ArrayList<>();
	for (DataFlowNode root : dfg.getSubgraphRoots()) {
	    DataFlowSubgraph sub = dfg.getSubgraph(root);
	    DataFlowSubgraphMetrics m = sub.getMetrics();
	    m.setIterations(DFGUtils.estimateNodeFrequency(root));
	    metrics.add(m);
	}
	return DFGUtils.sumArrayLoads(metrics);
    }

    @Override
    public void apply() {
	canInline.forEach((funName, canInline) -> {
	    if (canInline) {
		HLSInline pragma = new HLSInline(InlineType.NONE);
		ClavaNode astNode = functions.get(funName).getFirstStmt();
		insertDirective(astNode, pragma);
		ClavaHLS.log("inlining function \"" + funName + "\"");
	    } else
		ClavaHLS.log("function \"" + funName + "\" is not fit to be inlined");
	});
    }

}
