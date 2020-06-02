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
import pt.up.fe.specs.clava.analysis.flow.FlowEdge;
import pt.up.fe.specs.clava.analysis.flow.FlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DFGUtils;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowEdge;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowEdgeType;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNodeType;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowParam;
import pt.up.fe.specs.clava.hls.ClavaHLS;
import pt.up.fe.specs.clava.hls.directives.HLSStream;

public class ArrayStreamDetector extends RestructuringStrategy {
    private HashMap<String, Boolean> isStream = new HashMap<>();
    private final int OVERFLOW = 100000;

    public ArrayStreamDetector(DataFlowGraph dfg) {
	super(dfg);
	for (DataFlowParam param : dfg.getParams()) {
	    if (param.isArray())
		isStream.put(param.getName(), false);
	}

    }

    @Override
    public void analyze() {
	for (String variable : isStream.keySet()) {
	    ArrayList<DataFlowNode> loads = findLoads(variable);
	    if (loads.size() > 0) {
		boolean detected = analyzeLoads(loads);
		if (detected)
		    isStream.put(variable, true);
	    }
	}
    }

    private ArrayList<DataFlowNode> findLoads(String variable) {
	ArrayList<DataFlowNode> nodes = new ArrayList<>();
	boolean invalid = false;
	for (FlowNode n : dfg.getNodes()) {
	    DataFlowNode node = (DataFlowNode) n;
	    if (node.getLabel() == variable) {
		if (DataFlowNodeType.isStore(node.getType())) {
		    invalid = true;
		}
		if (DataFlowNodeType.isLoad(node.getType())) {
		    nodes.add(node);
		}
	    }
	}
	if (invalid)
	    nodes.clear();
	return nodes;
    }

    private boolean analyzeLoads(ArrayList<DataFlowNode> nodes) {
	// TODO: edge case of more than one load
	if (nodes.size() != 1)
	    return false;
	DataFlowNode node = nodes.get(0);

	// undetermined iterations -> undetermined access pattern
	if (node.getIterations() == Integer.MAX_VALUE)
	    return false;

	// Get indexes of access
	ArrayList<String> indexes = new ArrayList<>();
	for (FlowEdge e : node.getInEdges()) {
	    DataFlowEdge edge = (DataFlowEdge) e;
	    if (edge.getType() == DataFlowEdgeType.DATAFLOW_INDEX) {
		DataFlowNode idx = (DataFlowNode) edge.getSource();
		if (idx.getType() == DataFlowNodeType.LOAD_INDEX) {
		    indexes.add(idx.getLabel());
		} else
		    return false;
	    }
	}
	if (indexes.size() == 0)
	    return false;

	// Check if each index matches the parent loops
	// Collections.reverse(indexes);
	for (int i = indexes.size() - 1; i >= 0; i--) {
	    int dist = getDistanceToLoop(indexes.get(i), node);
	    if (dist != i)
		return false;
	}
	return true;

    }

    private int getDistanceToLoop(String idx, DataFlowNode node) {
	DataFlowNode loop = DFGUtils.getLoopOfNode(node);
	if (loop == node)
	    return OVERFLOW;
	if (loop.getLabel().equals("loop " + idx))
	    return 0;
	else
	    return 1 + getDistanceToLoop(idx, loop);
    }

    public int detectedCases() {
	int count = 0;
	for (boolean b : isStream.values()) {
	    count += b ? 1 : 0;
	}
	return count;
    }

    @Override
    public void apply() {
	ClavaNode node = dfg.getFirstStmt();

	for (String variable : isStream.keySet()) {
	    if (isStream.get(variable)) {
		HLSStream directive = new HLSStream(variable);
		insertDirective(node, directive);
		DataFlowParam param = DFGUtils.getParamByName(dfg, variable);
		param.setStream(true);
		ClavaHLS.log("declaring parameter array \"" + variable + "\" as stream");
	    }
	}
    }
}
