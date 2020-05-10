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
import java.util.Iterator;

import pt.up.fe.specs.clava.analysis.flow.FlowEdge;
import pt.up.fe.specs.clava.analysis.flow.FlowNode;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlockEdge;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlockNode;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlockNodeType;
import pt.up.fe.specs.clava.analysis.flow.control.ControlFlowGraph;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

public class CFGConverter {
    public static void convert(ControlFlowGraph cfg) {
	removeComments(cfg);
	// normalizeEdges(cfg);
	cfg.generateDot(false);
    }

    private static void removeComments(ControlFlowGraph cfg) {
	for (FlowNode n : cfg.getNodes()) {
	    BasicBlockNode node = (BasicBlockNode) n;
	    Iterator<Stmt> itr = node.getStmts().iterator();
	    while (itr.hasNext()) {
		Stmt s = itr.next();
		if (s.isWrapper()) {
		    itr.remove();
		}
	    }
	}
    }

    private static void normalizeEdges(ControlFlowGraph cfg) {
	ArrayList<FlowEdge> toRemove = new ArrayList<>();
	for (FlowEdge e : cfg.getEdges()) {
	    BasicBlockEdge edge = (BasicBlockEdge) e;
	    BasicBlockNode source = (BasicBlockNode) edge.getSource();
	    BasicBlockNode dest = (BasicBlockNode) edge.getDest();
	    if (source.getType() == BasicBlockNodeType.NORMAL && dest.getType() == BasicBlockNodeType.LOOP) {
		source.removeOutEdge(e);
		dest.removeInEdge(e);
		toRemove.add(e);
	    }
	}
	for (FlowEdge e : toRemove)
	    cfg.removeEdge(e);
    }
}
