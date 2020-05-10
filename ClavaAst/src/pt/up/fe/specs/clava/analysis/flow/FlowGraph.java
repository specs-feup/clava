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
    protected ArrayList<FlowNode> nodes = new ArrayList<>();
    protected ArrayList<FlowEdge> edges = new ArrayList<>();
    protected String name;
    protected final String nodePrefix;
    protected int nodeID = 0;

    public FlowGraph(String name, String nodePrefix) {
	this.name = name;
	this.nodePrefix = nodePrefix;
    }

    public void addNode(FlowNode node) {
	nodes.add(node);
	node.initNode(nodePrefix, nodeID);
	nodeID++;
    }

    public void addEdge(FlowEdge edge) {
	edges.add(edge);
	FlowNode source = edge.getSource();
	FlowNode dest = edge.getDest();
	for (FlowNode node : nodes) {
	    if (node.getId() == source.getId())
		node.addOutEdge(edge);
	    if (node.getId() == dest.getId())
		node.addInEdge(edge);
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

    public ArrayList<FlowNode> getNodes() {
	return nodes;
    }

    public ArrayList<FlowEdge> getEdges() {
	return edges;
    }

    protected ArrayList<FlowNode> getSources() {
	ArrayList<FlowNode> sources = new ArrayList<>();
	for (FlowNode node : nodes)
	    if (node.getInNodes().size() == 0)
		sources.add(node);
	return sources;
    }

    protected ArrayList<FlowNode> getSinks() {
	ArrayList<FlowNode> sinks = new ArrayList<>();
	for (FlowNode node : nodes)
	    if (node.getOutNodes().size() == 0)
		sinks.add(node);
	return sinks;
    }

    public FlowNode findNode(int id) {
	for (FlowNode node : nodes)
	    if (node.getId() == id)
		return node;
	return null;
    }

    public void removeNode(FlowNode node) {
	this.nodes.remove(node);
    }

    public void removeEdge(FlowEdge edge) {
	System.out.println(this.edges.contains(edge));
	if (this.edges.contains(edge))
	    this.edges.remove(edge);
    }
}
