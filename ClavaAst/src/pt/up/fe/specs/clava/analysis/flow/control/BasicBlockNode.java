/**
 * Copyright 2020 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.analysis.flow.control;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.specs.clava.analysis.flow.FlowNode;
import pt.up.fe.specs.clava.ast.stmt.DoStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.WhileStmt;

public class BasicBlockNode extends FlowNode {

    private Stmt leader;
    private List<Stmt> stmts;
    private BasicBlockNodeType type;

    public BasicBlockNode(Stmt leader) {
	super("BB");
	label += id;
	this.leader = leader;
	stmts = new ArrayList<Stmt>();
	stmts.add(leader);
	type = BasicBlockNodeType.UNDEFINED;
    }

    public Stmt first() {
	return leader;
    }

    public Stmt last() {
	return stmts.get(stmts.size() - 1);
    }

    public void addStmt(Stmt stmt) {
	stmts.add(stmt);
    }

    public void setType(BasicBlockNodeType type) {
	this.type = type;
    }

    @Override
    public String toString() {

	StringBuilder builder = new StringBuilder();

	builder.append(id);
	builder.append(" (");
	builder.append(type);
	builder.append(") ");
	// builder.append(outEdges.stream().map(BasicBlockNode::getId).collect(Collectors.toList()));
	builder.append("\n");

	for (Stmt stmt : stmts) {

	    builder.append(stmt.get(stmt.LOCATION).getStartLine()).append(": ");

	    builder.append(getBbCode(stmt));

	    builder.append("\n");
	}

	return builder.toString();
    }

    @Override
    public String toDot() {

	String NL = "\n";
	String LEFT = "\\l";
	String QUOTE = "\"";
	String SELF = "\\N";
	String ARROW = "->";

	StringBuilder builder = new StringBuilder();
	/*
	 * for (BasicBlock bb : outEdges) { builder.append(id); builder.append(ARROW);
	 * builder.append(bb.id); builder.append(NL); }
	 */
	builder.append(label).append(" ");
	builder.append("[label=");
	builder.append(QUOTE);
	builder.append(SELF);
	builder.append(":");
	builder.append(LEFT);
	for (Stmt stmt : stmts) {

	    builder.append(getBbCode(stmt));
	    builder.append(LEFT);
	}
	builder.append(QUOTE);
	builder.append("];");
	builder.append(NL);

	return builder.toString();
    }

    private String getBbCode(Stmt stmt) {

	StringBuilder builder = new StringBuilder();

	if (stmt instanceof IfStmt) {

	    String cond = ((IfStmt) stmt).getCondition().getCode();
	    builder.append("if(").append(cond).append(")");

	    return builder.toString();
	}

	if (stmt instanceof ForStmt) {

	    ForStmt f = (ForStmt) stmt;

	    builder.append("for").append("(");

	    builder.append(f.getInit().map(init -> init.getCode()).orElse(";"));
	    builder.append(f.getCond().map(init -> " " + init.getCode()).orElse(";"));
	    String incCode = f.getInc().map(init -> " " + init.getCode()).orElse("");
	    if (incCode.endsWith(";")) {
		incCode = incCode.substring(0, incCode.length() - 1);
	    }
	    builder.append(incCode);
	    builder.append(")");

	    return builder.toString();
	}

	if (stmt instanceof WhileStmt) {

	    String cond = ((WhileStmt) stmt).getCondition().getCode();
	    builder.append("while").append("(").append(cond).append(")");

	    return builder.toString();
	}

	if (stmt instanceof DoStmt) {

	    String cond = ((DoStmt) stmt).getCondition().getCode();
	    builder.append("while").append("(").append(cond).append(")");

	    return builder.toString();
	}

	return stmt.getCode().trim();
    }

    public BasicBlockNodeType getType() {
	return type;
    }

    public boolean hasOutEdges() {
	return !outEdges.isEmpty();
    }

    public boolean hasInEdges() {
	return !inEdges.isEmpty();
    }

    public Stmt getLeader() {
	return leader;
    }

    public List<Stmt> getStmts() {
	return stmts;
    }
}
