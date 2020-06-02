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
import java.util.Stack;

import pt.up.fe.specs.clava.analysis.flow.FlowEdge;
import pt.up.fe.specs.clava.analysis.flow.FlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DFGUtils;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowEdge;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowEdgeType;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNodeType;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.hls.ClavaHLS;
import pt.up.fe.specs.clava.hls.directives.HLSUnroll;
import pt.up.fe.specs.clava.hls.heuristics.UnrollHeuristic;

public class NestedLoopUnrolling extends RestructuringStrategy {
    private HashMap<DataFlowNode, Integer> loopsToUnroll = new HashMap<>();

    public NestedLoopUnrolling(DataFlowGraph dfg) {
	super(dfg);

    }

    @Override
    public void analyze() {
	ArrayList<DataFlowNode> masterLoops = findMasterLoops();
	for (DataFlowNode masterLoop : masterLoops) {
	    ArrayList<DataFlowNode> bottomLoops = getBottomLoops(masterLoop);
	    for (DataFlowNode bottomLoop : bottomLoops) {
		ClavaHLS.log("trying to unroll loop nest starting by bottom loop \"" + bottomLoop.getLabel()
			+ "\" (trip count = " + bottomLoop.getIterations() + ")");
		evaluateLoopNest(bottomLoop);
	    }
	}
    }

    private void evaluateLoopNest(DataFlowNode loop) {
	this.loopsToUnroll.put(loop, Integer.MAX_VALUE);
	DataFlowNode parent = DFGUtils.getLoopOfLoop(loop);
	while (parent != loop) {
	    if (UnrollHeuristic.calculate(parent, loop)) {
		this.loopsToUnroll.put(parent, Integer.MAX_VALUE);
		loop = parent;
		parent = DFGUtils.getLoopOfLoop(loop);
	    } else
		return;
	}
    }

    @Override
    public void apply() {
	ClavaHLS.log("unrolling " + loopsToUnroll.size() + " loops");
	for (DataFlowNode node : loopsToUnroll.keySet()) {
	    int factor = loopsToUnroll.get(node);
	    Stmt stmt = node.getStmt();
	    HLSUnroll directive = new HLSUnroll();
	    StringBuilder sb = new StringBuilder("unrolling \"");
	    sb.append(node.getLabel()).append("\" (trip count = ").append(node.getIterations()).append(") ");

	    if (factor != Integer.MAX_VALUE) {
		directive.setFactor(factor);
		sb.append("with factor = ").append(factor);
	    } else {
		sb.append("fully");
	    }
	    insertDirective(stmt, directive);
	    ClavaHLS.log(sb.toString());
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

    private ArrayList<DataFlowNode> getBottomLoops(DataFlowNode masterLoop) {
	ArrayList<DataFlowNode> loops = new ArrayList<>();
	Stack<DataFlowNode> nodes = new Stack<>();
	nodes.add(masterLoop);
	while (!nodes.isEmpty()) {
	    DataFlowNode node = nodes.pop();
	    int cnt = 0;
	    for (FlowEdge e : node.getOutEdges()) {
		DataFlowEdge edge = (DataFlowEdge) e;
		if (edge.getType() == DataFlowEdgeType.CONTROL_REPEATING) {
		    if (((DataFlowNode) edge.getDest()).getType() == DataFlowNodeType.LOOP) {
			cnt++;
			nodes.add((DataFlowNode) edge.getDest());
		    }
		}
	    }
	    if (cnt == 0)
		loops.add(node);
	}
	return loops;
    }

    public HashMap<DataFlowNode, Integer> getLoopsToUnroll() {
	return loopsToUnroll;
    }
}
