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

import java.util.ArrayList;

/**
 * Abstract class that represents a generic flow graph. It is a directed graph
 * by definition
 * 
 * @author Tiago
 *
 */
public abstract class FlowGraph implements ToDot {
    protected ArrayList<FlowNode> nodes = new ArrayList<>();
    protected ArrayList<FlowEdge> edges = new ArrayList<>();
    protected String name;
    protected final String nodePrefix;
    protected int nodeID = 0;

    public FlowGraph(String name, String nodePrefix) {
	this.name = name;
	this.nodePrefix = nodePrefix;
    }

    /**
     * Adds a new node to the graph, creating a new ID for that node
     * 
     * @param node
     */
    public void addNode(FlowNode node) {
	nodes.add(node);
	node.initNode(nodePrefix, nodeID);
	nodeID++;
    }

    /**
     * Adds a new edge to the graph, updating the adjacency lists of the edge's
     * source and destination nodes
     * 
     * @param edge
     */
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

    @Override
    public abstract String toDot();

    /**
     * Gets the graph's nodes
     * 
     * @return a list of nodes
     */
    public ArrayList<FlowNode> getNodes() {
	return nodes;
    }

    /**
     * Gets the graph's edges
     * 
     * @return a list of edges
     */
    public ArrayList<FlowEdge> getEdges() {
	return edges;
    }

    /**
     * Gets the graph's sources
     * 
     * @return a list of nodes
     */
    protected ArrayList<FlowNode> getSources() {
	ArrayList<FlowNode> sources = new ArrayList<>();
	for (FlowNode node : nodes)
	    if (node.getInNodes().size() == 0)
		sources.add(node);
	return sources;
    }

    /**
     * Gets the graph's sinks
     * 
     * @return a list of nodes
     */
    protected ArrayList<FlowNode> getSinks() {
	ArrayList<FlowNode> sinks = new ArrayList<>();
	for (FlowNode node : nodes)
	    if (node.getOutNodes().size() == 0)
		sinks.add(node);
	return sinks;
    }

    /**
     * Fetches the node with the specified ID
     * 
     * @param id
     * @return the node with the specified ID if it exists; null otherwise
     */
    public FlowNode findNode(int id) {
	for (FlowNode node : nodes)
	    if (node.getId() == id)
		return node;
	return null;
    }

    /**
     * Removes the specified node from the graph
     * 
     * @param node
     */
    public void removeNode(FlowNode node) {
	this.nodes.remove(node);
    }

    /**
     * Removes the specified edge from the graph
     * 
     * @param edge
     */
    public void removeEdge(FlowEdge edge) {
	System.out.println(this.edges.contains(edge));
	if (this.edges.contains(edge))
	    this.edges.remove(edge);
    }
}
