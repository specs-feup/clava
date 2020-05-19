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

import pt.up.fe.specs.clava.analysis.flow.FlowEdge;
import pt.up.fe.specs.clava.analysis.flow.FlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowEdge;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNodeType;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.hls.ClavaHLS;
import pt.up.fe.specs.clava.hls.directives.HLSUnroll;

public class NestedLoopUnrolling extends RestructuringStrategy {
    private HashMap<DataFlowNode, Integer> loopsToUnroll = new HashMap<>();

    public NestedLoopUnrolling(DataFlowGraph dfg) {
	super(dfg);

    }

    @Override
    public void analyze() {
	ArrayList<DataFlowNode> loops = findMasterLoops();
	for (DataFlowNode loop : loops)
	    evaluateLoopNest(loop);

    }

    private long evaluateLoopNest(DataFlowNode loop) {
	boolean hasNested = false;
	long currFactor = 0;
	for (FlowNode n : loop.getOutNodes()) {
	    DataFlowNode node = (DataFlowNode) n;
	    if (node.getType() == DataFlowNodeType.LOOP) {
		hasNested = true;
		long prevFactor = evaluateLoopNest(node);
		if (prevFactor == Integer.MAX_VALUE)
		    return Integer.MAX_VALUE;
	    }
	}
	if (!hasNested) {
	    long iter = loop.getIterations();
	    if (iter != Integer.MAX_VALUE)
		currFactor = iter;
	}
	return currFactor;
    }

    @Override
    public void apply() {
	for (DataFlowNode node : loopsToUnroll.keySet()) {
	    int factor = loopsToUnroll.get(node);
	    Stmt stmt = node.getStmt();
	    HLSUnroll directive = new HLSUnroll();
	    if (factor != Integer.MAX_VALUE)
		directive.setFactor(factor);
	    insertDirective(stmt, directive);
	}

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
		    StringBuilder sb = new StringBuilder();
		    sb.append("found master loop \"").append(node.getLabel()).append("\"");
		    if (node.getIterations() != Integer.MAX_VALUE) {
			sb.append(" (trip count = ").append(node.getIterations()).append(")");
		    } else
			sb.append(" (undetermined trip count)");
		    ClavaHLS.log(sb.toString());
		}
	    }
	}
	return loops;
    }

}
