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
    private int id = -1;

    public DataFlowSubgraph(DataFlowNode root) {
	this.root = root;
	this.id = root.getSubgraphID();
    }

    public ArrayList<DataFlowNode> getNodes() {
	return findNodes(root);
    }

    private ArrayList<DataFlowNode> findNodes(DataFlowNode node) {
	ArrayList<DataFlowNode> nodes = new ArrayList<>();
	for (FlowNode n : node.getInNodes()) {
	    DataFlowNode ascendant = (DataFlowNode) n;
	    nodes.addAll(findNodes(ascendant));
	}
	return nodes;
    }

    public void mergeNodes() {

    }

    public DataFlowSubgraphMetrics getMetrics() {
	DataFlowSubgraphMetrics metrics = new DataFlowSubgraphMetrics(root);
	// metrics.setCriticalPath(calculateCriticalPath());
	metrics.setDepth(calculateDepth(root, 0));
	// metrics.setLoads(findLoads());
	// metrics.setStores(findStores());
	return metrics;
    }

    private HashMap<String, Integer> findStores() {
	// TODO Auto-generated method stub
	return null;
    }

    private HashMap<String, Integer> findLoads() {
	// TODO Auto-generated method stub
	return null;
    }

    private int calculateDepth(DataFlowNode node, int depth) {
//	if (node.getInNodes().size() == 0)
//	    return depth;
//	else {
//	    ArrayList<FlowNode> children = node.getInNodes();
//	    int max = 0;
//	    for (FlowNode child : children) {
//		DataFlowNode childNode = (DataFlowNode) child;
//		int d = calculateDepth(childNode, depth + 1);
//		if (d > max)
//		    max = d;
//	    }
//	    return max;
//	}
	return 0;
    }

    private ArrayList<DataFlowNode> calculateCriticalPath() {
	// TODO Auto-generated method stub
	return this.getNodes();
    }
}
