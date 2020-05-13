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

import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNodeType;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.hls.ClavaHLS;
import pt.up.fe.specs.clava.hls.DFGUtils;

public class FunctionInlining extends RestructuringStrategy {
    private HashMap<DataFlowNode, Integer> callFreq;
    private HashMap<String, DataFlowGraph> functions;
    private HashMap<DataFlowNode, Boolean> inline;

    public FunctionInlining(DataFlowGraph dfg) {
	super(dfg);
	callFreq = new HashMap<>();
	functions = new HashMap<>();
	inline = new HashMap<>();
    }

    @Override
    public void analyze() {
	ArrayList<DataFlowNode> calls = DFGUtils.getAllNodesOfType(dfg, DataFlowNodeType.OP_CALL);
	findDistinctFunctions(calls);
	estimateCallFrequencies(calls);
    }

    private void findDistinctFunctions(ArrayList<DataFlowNode> calls) {
	for (DataFlowNode call : calls) {
	    String funName = call.getLabel();
	    if (!functions.containsKey(funName)) {
		functions.put(funName, getCallDfg(call));
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
	funDFG.generateDot(false);
	return funDFG;
    }

    private void estimateCallFrequencies(ArrayList<DataFlowNode> calls) {
	for (DataFlowNode call : calls) {
	    callFreq.put(call, DFGUtils.estimateNodeFrequency(call));
	}
    }

    public void printFrequencies() {
	for (DataFlowNode node : callFreq.keySet()) {
	    int freq = callFreq.get(node);
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

    @Override
    public void apply() {
	// TODO Auto-generated method stub

    }

}
