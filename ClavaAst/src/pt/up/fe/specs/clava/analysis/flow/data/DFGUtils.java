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

package pt.up.fe.specs.clava.analysis.flow.data;

import java.util.ArrayList;
import java.util.Stack;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.analysis.flow.FlowEdge;
import pt.up.fe.specs.clava.analysis.flow.FlowNode;

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
	    if (edge.getType() == DataFlowEdgeType.DATAFLOW_INDEX)
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

    public static int sumArrayLoads(ArrayList<DataFlowSubgraphMetrics> metrics) {
	int sum = 0;
	for (DataFlowSubgraphMetrics m : metrics)
	    sum += m.getNumArrayLoads() * m.getIterations();
	return sum;
    }

    public static ArrayList<DataFlowNode> getVarLoadsOfSubgraph(DataFlowSubgraph sub) {
	ArrayList<DataFlowNode> nodes = new ArrayList<>();
	for (DataFlowNode node : sub.getNodes()) {
	    if (node.getType() == DataFlowNodeType.LOAD_VAR)
		nodes.add(node);
	}
	return nodes;
    }

    public static boolean isIndex(DataFlowNode node) {
	String name = node.getLabel();
	DataFlowNode loop = getLoopOfNode(node);
	if (loop == node)
	    return false;
	while (loop != node) {
	    if (getIteratorOfLoop(loop).equals(name))
		return true;
	    else {
		node = loop;
		loop = getLoopOfLoop(node);
	    }
	}
	return false;
    }

    public static String getIteratorOfLoop(DataFlowNode loop) {
	String tokens[] = loop.getLabel().split(" ");
	return tokens.length == 2 ? tokens[1] : "__undefined";
    }

    public static boolean isSameArrayAccess(DataFlowNode n1, DataFlowNode n2) {
	ArrayList<DataFlowNode> f1 = getIndexExpr(n1);
	ArrayList<DataFlowNode> f2 = getIndexExpr(n2);
	return compareFlows(f1, f2);
    }

    public static ArrayList<DataFlowNode> getIndexExpr(DataFlowNode n) {
	ArrayList<DataFlowNode> nodes = new ArrayList<>();
	Stack<DataFlowNode> s = new Stack<>();
	s.add(n);
	while (!s.isEmpty()) {
	    DataFlowNode node = s.pop();
	    if (!nodes.contains(node)) {
		nodes.add(node);
		for (FlowNode child : node.getInNodes()) {
		    s.add((DataFlowNode) child);
		}
	    }
	}
	return nodes;
    }

    public static boolean compareFlows(ArrayList<DataFlowNode> f1, ArrayList<DataFlowNode> f2) {
	if (f1.size() != f2.size())
	    return false;
	for (int i = 0; i < f1.size(); i++) {
	    if (f1.get(i).getLabel() != f2.get(i).getLabel())
		return false;
	}
	return true;
    }
}
