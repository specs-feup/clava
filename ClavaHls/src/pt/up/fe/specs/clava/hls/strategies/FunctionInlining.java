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
    private HashMap<DataFlowNode, Long> callFreq;
    private HashMap<String, DataFlowGraph> functions;
    private HashMap<String, Boolean> isFunctionInlinable;
    private HashMap<String, Long> functionCosts;
    private HashMap<DataFlowNode, Boolean> isCallInlinable;

    public FunctionInlining(DataFlowGraph dfg) {
	super(dfg);
	callFreq = new HashMap<>();
	functions = new HashMap<>();
	isFunctionInlinable = new HashMap<>();
	functionCosts = new HashMap<>();
	isCallInlinable = new HashMap<>();
    }

    @Override
    public void analyze() {
	ArrayList<DataFlowNode> calls = DFGUtils.getAllNodesOfType(dfg, DataFlowNodeType.OP_CALL);

	// Check if called functions are inlinable
	findDistinctFunctions(calls);

	// Remove calls to non-inlinable functions
	isFunctionInlinable.forEach((k, v) -> {
	    if (!v)
		calls.removeIf(n -> n.getLabel() == k);
	});

	// Check if each call is inlinable
	long mainFunCost = analyzeFunction(dfg);
	estimateCallFrequencies(calls);

	for (DataFlowNode call : calls) {
	    long freq = callFreq.get(call);
	    long funCost = functionCosts.get(call.getLabel());

	    // HEURISTIC HERE
	    boolean inline = InlineHeuristic.calculate(freq, funCost, mainFunCost);
	    isCallInlinable.put(call, inline);
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
	    ClavaHLS.log("function defininion of \"" + call.getLabel() + "\" not found");
	    return null;
	}
	FunctionDecl fun = callNode.getDefinition().get();

	if (!fun.getBody().isPresent()) {
	    ClavaHLS.log("function body of \"" + call.getLabel() + "\" not found");
	    return null;
	}
	CompoundStmt scope = fun.getBody().get();

	DataFlowGraph funDFG = new DataFlowGraph(scope);
	System.out.println(funDFG.toDot());
	return funDFG;
    }

    private void estimateCallFrequencies(ArrayList<DataFlowNode> calls) {
	for (DataFlowNode call : calls) {
	    callFreq.put(call, DFGUtils.estimateNodeFrequency(call));
	}
    }

    public void printFrequencies() {
	for (DataFlowNode node : callFreq.keySet()) {
	    long freq = callFreq.get(node);
	    StringBuilder sb = new StringBuilder();
	    sb.append("Call to \"").append(node.getLabel()).append("\" (l:")
		    .append(node.getStmt().getLocation().getStartLine()).append(")");
	    if (freq == Integer.MAX_VALUE)
		sb.append(" is executed an undetermined number of times");
	    else
		sb.append(" is executed ").append(freq).append(" time(s)");
	    ClavaHLS.log(sb.toString());
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
//	for (DataFlowSubgraphMetrics m : metrics)
//	    System.out.println(m.toString());
	return DFGUtils.sumArrayLoads(metrics);
    }

    @Override
    public void apply() {
	isCallInlinable.forEach((k, v) -> {
	    if (v) {
		HLSInline pragma = new HLSInline(InlineType.NONE);
		insertDirective(k.getStmt(), pragma);
		ClavaHLS.log("Inlining call to \"" + k.getLabel() + "\" (l:" + k.getStmt().getLocation().getStartLine()
			+ ")");
	    }
	});
    }

}
