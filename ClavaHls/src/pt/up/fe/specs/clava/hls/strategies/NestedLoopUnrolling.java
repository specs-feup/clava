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

import pt.up.fe.specs.clava.analysis.flow.FlowEdge;
import pt.up.fe.specs.clava.analysis.flow.FlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowEdge;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNodeType;
import pt.up.fe.specs.clava.hls.ClavaHLS;

public class NestedLoopUnrolling extends RestructuringStrategy {

    public NestedLoopUnrolling(DataFlowGraph dfg) {
	super(dfg);
	// TODO Auto-generated constructor stub
    }

    @Override
    public void analyze() {
	ArrayList<DataFlowNode> loops = findMasterLoops();

    }

    @Override
    public void apply() {
	// TODO Auto-generated method stub

    }

    private ArrayList<DataFlowNode> findMasterLoops() {
	ArrayList<DataFlowNode> loops = new ArrayList<>();
	for (FlowNode n : dfg.getNodes()) {
	    DataFlowNode node = (DataFlowNode) n;
	    if (node.getType() == DataFlowNodeType.LOOP) {
		boolean isNested = false;
		for (FlowEdge e : node.getInEdges()) {
		    DataFlowEdge edge = (DataFlowEdge) e;
		    if (edge.repeating > 0)
			isNested = true;
		}
		if (!isNested) {
		    loops.add(node);
		    ClavaHLS.log("found master loop \"" + node.getLabel() + "\"");
		}
	    }
	}
	return loops;
    }

}
