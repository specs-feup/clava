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

import pt.up.fe.specs.clava.analysis.flow.FlowEdge;
import pt.up.fe.specs.clava.analysis.flow.FlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowEdge;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowEdgeType;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNodeType;

public class DFGUtils {
    public static DataFlowNode getLoopOfNode(DataFlowNode node) {
	while (node.getType() != DataFlowNodeType.LOOP) {
	    if (DataFlowNodeType.isStore(node.getType())) {
		for (FlowNode n : node.getInNodes()) {
		    DataFlowNode curr = (DataFlowNode) n;
		    if (curr.getType() == DataFlowNodeType.LOOP)
			node = curr;
		}
	    } else {
		node = (DataFlowNode) node.getOutNodes().get(0);
	    }
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
}
