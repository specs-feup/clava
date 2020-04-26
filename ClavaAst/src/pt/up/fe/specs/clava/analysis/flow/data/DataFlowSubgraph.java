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

    public DataFlowSubgraph(DataFlowNode root) {
	this.root = root;
    }

    public ArrayList<DataFlowNode> getNodes() {
	ArrayList<DataFlowNode> nodes = findNodes(root);
	nodes.forEach(node -> {
	    node.setExplored(false);
	});
	return nodes;
    }

    private ArrayList<DataFlowNode> findNodes(DataFlowNode node) {
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
	int depth = calculateCriticalPath(root);
	ArrayList<DataFlowNode> path = root.getCurrPath();
	metrics.setCriticalPath(path);
	metrics.setDepth(path.size());
	metrics.setNumLoads(findLoads());
	metrics.setNumStores(findStores());
	metrics.setNumOp(findOps());
	return metrics;
    }

    private int findOps() {
	ArrayList<DataFlowNode> nodes = this.getNodes();
	int counter = 0;
	for (DataFlowNode node : nodes) {
	    if (node.getType() == DataFlowNodeType.OP_ARITH || node.getType() == DataFlowNodeType.OP_CALL) {
		counter++;
	    }
	}
	return counter;
    }

    private int findStores() {
	ArrayList<DataFlowNode> nodes = this.getNodes();
	int counter = 0;
	for (DataFlowNode node : nodes) {
	    if (node.getType() == DataFlowNodeType.STORE_ARRAY || node.getType() == DataFlowNodeType.STORE_VAR) {
		counter++;
	    }
	}
	return counter;
    }

    private int findLoads() {
	ArrayList<DataFlowNode> nodes = this.getNodes();
	int counter = 0;
	for (DataFlowNode node : nodes) {
	    if (node.getType() == DataFlowNodeType.LOAD_ARRAY || node.getType() == DataFlowNodeType.LOAD_VAR
		    || node.getType() == DataFlowNodeType.LOAD_INDEX) {
		counter++;
	    }
	}
	return counter;
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

    private ArrayList<DataFlowNode> getSources() {
	ArrayList<DataFlowNode> sources = new ArrayList<>();
	ArrayList<DataFlowNode> nodes = getNodes();
	nodes.forEach(node -> {
	    if (node.getInEdges().size() == 0 && node.getOutEdges().size() != 0)
		sources.add(node);
	});
	return sources;
    }
}
