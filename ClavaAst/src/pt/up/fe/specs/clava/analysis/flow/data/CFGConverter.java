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

import java.util.Iterator;

import pt.up.fe.specs.clava.analysis.flow.FlowNode;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlockNode;
import pt.up.fe.specs.clava.analysis.flow.control.ControlFlowGraph;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

public class CFGConverter {
    public static void convert(ControlFlowGraph cfg) {
	removeComments(cfg);
    }

    private static void removeComments(ControlFlowGraph cfg) {
	for (FlowNode n : cfg.getNodes()) {
	    BasicBlockNode node = (BasicBlockNode) n;
	    Iterator<Stmt> itr = node.getStmts().iterator();
	    while (itr.hasNext()) {
		Stmt s = itr.next();
		if (s.isWrapper()) {
		    if (!isValidPragma(s.getCode()))
			itr.remove();
		}
	    }
	}
    }

    private static boolean isValidPragma(String code) {
	return parsePragma(code) != -1;
    }

    public static int parsePragma(String code) {
	String[] tokens = code.replace("\n", "").split(" ");
	if (tokens.length == 3) {
	    if (!tokens[0].toLowerCase().equals("#pragma"))
		return -1;
	    if (!tokens[1].toLowerCase().equals("max_iter"))
		return -1;
	    try {
		int i = Integer.parseInt(tokens[2].strip());
		return i;
	    } catch (NumberFormatException e) {
		return -1;
	    }
	}
	return -1;
    }
}
