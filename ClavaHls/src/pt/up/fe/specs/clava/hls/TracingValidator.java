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

import pt.up.fe.specs.clava.analysis.flow.FlowEdge;
import pt.up.fe.specs.clava.analysis.flow.FlowNode;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlockEdge;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlockEdgeType;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlockNode;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlockNodeType;
import pt.up.fe.specs.clava.analysis.flow.control.ControlFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DFGUtils;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNodeType;

public class TracingValidator {
    private DataFlowGraph dfg;

    public TracingValidator(DataFlowGraph dfg) {
	this.dfg = dfg;
    }

    public boolean validate() {
	if (!checkConditionals())
	    return false;
	if (!checkCalls())
	    return false;
	if (!checkAccessPatterns())
	    return false;
	return true;
    }

    private boolean checkConditionals() {
	ControlFlowGraph cfg = dfg.getCfg();
	for (FlowNode n : cfg.getNodes()) {
	    BasicBlockNode node = (BasicBlockNode) n;
	    if (node.getType() == BasicBlockNodeType.IF)
		return false;
	    for (FlowEdge e : node.getOutEdges()) {
		BasicBlockEdge edge = (BasicBlockEdge) e;
		if (edge.getType() == BasicBlockEdgeType.FALSE || edge.getType() == BasicBlockEdgeType.TRUE)
		    return false;
	    }
	}
	return true;
    }

    private boolean checkCalls() {
	for (DataFlowNode node : DFGUtils.getAllNodesOfType(dfg, DataFlowNodeType.OP_CALL)) {
	    if (node.getInEdges().size() != 1)
		return false;
	}
	return true;
    }

    private boolean checkAccessPatterns() {
	return true;
    }
}
