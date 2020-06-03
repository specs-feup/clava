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

package pt.up.fe.specs.clava.analysis.flow.control;

import java.util.ArrayList;
import java.util.Iterator;

import pt.up.fe.specs.clava.analysis.flow.FlowNode;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

public class CFGUtils {
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

    public static boolean hasBasicBlockOfType(ControlFlowGraph cfg, BasicBlockNodeType type) {
	for (FlowNode n : cfg.getNodes()) {
	    BasicBlockNode node = (BasicBlockNode) n;
	    if (node.getType() == type)
		return true;
	}
	return false;
    }

    public static ArrayList<BasicBlockNode> getTopLevelBlocks(ControlFlowGraph cfg) {
	ArrayList<BasicBlockNode> bbs = new ArrayList<>();
	for (FlowNode n : cfg.getNodes()) {
	    BasicBlockNode node = (BasicBlockNode) n;
	    if (isTopLevel(node))
		bbs.add(node);
	}
	return bbs;
    }

    public static BasicBlockNode getTopLevelIfDescendant(ControlFlowGraph cfg, BasicBlockNode ifBlock) {
	ArrayList<BasicBlockNode> topBBs = getTopLevelBlocks(cfg);
	// Possibly does not cover all cases
	int startId = ifBlock.getId();
	for (BasicBlockNode bb : topBBs) {
	    if (bb.getId() > startId)
		return bb;
	}
	return ifBlock;
    }

    public static boolean isTopLevel(BasicBlockNode node) {
	if (node.getStmts().size() > 0) {
	    Stmt stmt = node.getStmts().get(0);
	    if (stmt.getScope().isPresent()) {
		CompoundStmt scope = (CompoundStmt) stmt.getScope().get();
		if (scope.getParent() instanceof FunctionDecl) {
		    return true;
		}
	    }
	}
	return false;
    }
}
