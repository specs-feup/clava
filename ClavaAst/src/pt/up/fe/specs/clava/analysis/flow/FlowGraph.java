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

package pt.up.fe.specs.clava.analysis.flow;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

import pt.up.fe.specs.util.SpecsLogs;

public abstract class FlowGraph {
    protected ArrayList<Node> nodes = new ArrayList<>();
    protected ArrayList<Edge> edges = new ArrayList<>();
    protected String name;

    public FlowGraph(String name) {
	this.name = name;
    }

    public void addNode(Node node) {
	nodes.add(node);
    }

    public void addEdge(Edge edge) {
	edges.add(edge);
	Node source = edge.source;
	Node dest = edge.dest;
	for (Node node : nodes) {
	    if (node.getId() == source.getId())
		node.addInEdge(edge);
	    if (node.getId() == dest.getId())
		node.addOutEdge(edge);
	}
    }

    protected abstract String buildDot();

    public void generateDot(boolean saveToFile) {
	String dot = buildDot();
	if (saveToFile) {
	    try {
		PrintWriter file = new PrintWriter(name + ".dot");
		file.println(dot);
		file.close();
	    } catch (FileNotFoundException e) {
		SpecsLogs.msgWarn("Error message:\n", e);
	    }
	} else
	    System.out.println(dot);
    }
}
