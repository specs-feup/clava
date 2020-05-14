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

import java.util.ArrayList;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.analysis.flow.FlowEdge;
import pt.up.fe.specs.clava.analysis.flow.FlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowEdge;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowEdgeType;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNodeType;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowSubgraphMetrics;

public class DFGUtils {
    public static DataFlowNode getLoopOfNode(DataFlowNode node) {
	if (DataFlowNodeType.isStore(node.getType())) {
	    return getLoopOfLoop(node);
	}
	if (DataFlowNodeType.isLoop(node.getType())) {
	    return getLoopOfStore(node);
	}
	if (DataFlowNodeType.isLoad(node.getType()) || DataFlowNodeType.isOp(node.getType())) {
	    // TODO: Case for flows with no stores (e.g. calls)
	    return getLoopOfStore(getStoreOfNode(node));
	}
	return node;
    }

    public static DataFlowNode getLoopOfLoop(DataFlowNode node) {
	for (FlowNode n : node.getInNodes()) {
	    DataFlowNode curr = (DataFlowNode) n;
	    if (curr.getType() == DataFlowNodeType.LOOP)
		return curr;
	}
	return node;
    }

    public static DataFlowNode getLoopOfStore(DataFlowNode node) {
	for (FlowNode n : node.getInNodes()) {
	    DataFlowNode curr = (DataFlowNode) n;
	    if (curr.getType() == DataFlowNodeType.LOOP)
		return curr;
	}
	return node;
    }

    public static DataFlowNode getStoreOfNode(DataFlowNode node) {
	while (!DataFlowNodeType.isStore(node.getType())) {
	    node = (DataFlowNode) node.getOutNodes().get(0);
	}
	return node;
    }

    public static ArrayList<DataFlowNode> getIndexesOfArray(DataFlowNode node) {
	ArrayList<DataFlowNode> nodes = new ArrayList<>();
	for (FlowEdge e : node.getInEdges()) {
	    DataFlowEdge edge = (DataFlowEdge) e;
	    if (edge.getType() == DataFlowEdgeType.INDEX)
		nodes.add((DataFlowNode) edge.getSource());
	}
	return nodes;
    }

    public static ArrayList<DataFlowNode> getAllNodesOfType(DataFlowGraph dfg, DataFlowNodeType type) {
	ArrayList<DataFlowNode> nodes = new ArrayList<>();
	for (FlowNode n : dfg.getNodes()) {
	    DataFlowNode node = (DataFlowNode) n;
	    if (node.getType() == type)
		nodes.add(node);
	}
	nodes = (ArrayList<DataFlowNode>) nodes.stream().distinct().collect(Collectors.toList());
	return nodes;
    }

    public static int estimateNodeFrequency(DataFlowNode node) {
	int count = node.getIterations() == 0 ? 1 : node.getIterations();
	if (node.isTopLevel())
	    return count;
	DataFlowNode loop = getLoopOfNode(node);
	count *= loop.getIterations();
	while (!loop.isTopLevel()) {
	    loop = getLoopOfLoop(loop);
	    count *= loop.getIterations();
	}
	return count == 0 ? 1 : count;
    }

    public static int sumLoads(ArrayList<DataFlowSubgraphMetrics> metrics) {
	int sum = 0;
	for (DataFlowSubgraphMetrics m : metrics)
	    sum += m.getNumLoads() * m.getIterations();
	return sum;
    }
}
