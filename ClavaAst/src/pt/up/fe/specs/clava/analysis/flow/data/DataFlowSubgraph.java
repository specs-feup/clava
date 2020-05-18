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
import java.util.HashMap;

import pt.up.fe.specs.clava.analysis.flow.FlowNode;

public class DataFlowSubgraph {
    private DataFlowNode root;
    private int id;
    private DataFlowGraph dfg;

    public DataFlowSubgraph(DataFlowNode root, DataFlowGraph dfg) {
	this.root = root;
	this.dfg = dfg;
	this.id = root.getSubgraphID();
    }

    public ArrayList<DataFlowNode> getNodes() {
	ArrayList<DataFlowNode> nodes = findNodes(root);
	nodes.forEach(node -> {
	    node.setExplored(false);
	});
	return nodes;
    }

    @Deprecated
    private ArrayList<DataFlowNode> findNodesRecursively(DataFlowNode node) {
	ArrayList<DataFlowNode> nodes = new ArrayList<>();
	if (!node.isExplored()) {
	    nodes.add(node);
	    node.setExplored(true);
	    for (FlowNode n : node.getInNodes()) {
		DataFlowNode ascendant = (DataFlowNode) n;
		nodes.addAll(findNodes(ascendant));
	    }
	}
	return nodes;
    }

    private ArrayList<DataFlowNode> findNodes(DataFlowNode root) {
	ArrayList<DataFlowNode> nodes = new ArrayList<>();
	for (FlowNode n : dfg.getNodes()) {
	    DataFlowNode node = (DataFlowNode) n;
	    if (node.getSubgraphID() == this.id)
		nodes.add(node);
	}
	return nodes;
    }

    public HashMap<String, ArrayList<DataFlowNode>> getMultipleVarLoads() {
	HashMap<String, ArrayList<DataFlowNode>> map = new HashMap<>();
	ArrayList<DataFlowNode> nodes = getNodes();
	for (DataFlowNode node : nodes) {
	    if (node.getType() == DataFlowNodeType.LOAD_ARRAY || node.getType() == DataFlowNodeType.LOAD_VAR
		    || node.getType() == DataFlowNodeType.LOAD_INDEX) {
		if (map.containsKey(node.getLabel())) {
		    map.get(node.getLabel()).add(node);
		} else {
		    ArrayList<DataFlowNode> labelNodes = new ArrayList<>();
		    labelNodes.add(node);
		    map.put(node.getLabel(), labelNodes);
		}
	    }
	}
	map.entrySet().removeIf(e -> e.getValue().size() == 1);
	return map;
    }

    public DataFlowSubgraphMetrics getMetrics() {
	DataFlowSubgraphMetrics metrics = new DataFlowSubgraphMetrics(root);
	calculateCriticalPath(root);
	ArrayList<DataFlowNode> path = root.getCurrPath();
	metrics.setCriticalPath(path);
	metrics.setDepth(calculateDepth(path));

	metrics.setNumVarLoads(findLoads(false));
	metrics.setNumArrayLoads(findLoads(true));

	metrics.setNumVarStores(findStores(false));
	metrics.setNumArrayStores(findStores(true));

	metrics.setNumOp(findOps());
	metrics.setNumCalls(findCalls());
	metrics.setCode(root.getStmt().getCode());
	return metrics;
    }

    private int findOps() {
	ArrayList<DataFlowNode> nodes = this.getNodes();
	int counter = 0;
	for (DataFlowNode node : nodes) {
	    if (node.getType() == DataFlowNodeType.OP_ARITH) {
		counter++;
	    }
	}
	return counter;
    }

    private int findCalls() {
	ArrayList<DataFlowNode> nodes = this.getNodes();
	int counter = 0;
	for (DataFlowNode node : nodes) {
	    if (node.getType() == DataFlowNodeType.OP_CALL) {
		counter++;
	    }
	}
	return counter;
    }

    private int findStores(boolean array) {
	ArrayList<DataFlowNode> nodes = this.getNodes();
	int counter = 0;
	for (DataFlowNode node : nodes) {
	    if (array) {
		if (node.getType() == DataFlowNodeType.STORE_ARRAY)
		    counter++;
	    } else {
		if (node.getType() == DataFlowNodeType.STORE_VAR)
		    counter++;
	    }
	}
	return counter;
    }

    private int findLoads(boolean array) {
	ArrayList<DataFlowNode> nodes = this.getNodes();
	int counter = 0;
	for (DataFlowNode node : nodes) {
	    if (DataFlowNodeType.isLoad(node.getType())) {
		if (array) {
		    if (node.getType() == DataFlowNodeType.LOAD_ARRAY)
			counter++;
		} else {
		    if (node.getType() == DataFlowNodeType.LOAD_VAR || node.getType() == DataFlowNodeType.LOAD_INDEX)
			counter++;
		}
	    }
	}

	if (isRootAlsoALoad())
	    counter++;
	return counter;
    }

    private boolean isRootAlsoALoad() {
	return root.getOutEdges().size() == 1;
    }

    private int calculateCriticalPath(DataFlowNode node) {
	int count = 0;
	if (node.getType() == DataFlowNodeType.OP_ARITH || node.getType() == DataFlowNodeType.OP_CALL)
	    count++;
	node.getCurrPath().add(node);

	int max = -1;
	ArrayList<DataFlowNode> ascendantPath = new ArrayList<>();
	for (FlowNode n : node.getInNodes()) {
	    DataFlowNode ascendant = (DataFlowNode) n;
	    if (ascendant != root) {
		int ascendantCount = calculateCriticalPath(ascendant);
		if (ascendantCount > max) {
		    max = ascendantCount;
		    ascendantPath = ascendant.getCurrPath();
		}
	    }
	}
	max = (max == -1) ? 0 : max;
	node.getCurrPath().addAll(ascendantPath);
	return count + max;
    }

    private int calculateDepth(ArrayList<DataFlowNode> path) {
	int count = 0;
	for (DataFlowNode node : path) {
	    if (DataFlowNodeType.isOp(node.getType()))
		count++;
	}
	return count;
    }

    public int getId() {
	return id;
    }

    public void setId(int id) {
	this.id = id;
    }

    public DataFlowNode getRoot() {
	return root;
    }
}
