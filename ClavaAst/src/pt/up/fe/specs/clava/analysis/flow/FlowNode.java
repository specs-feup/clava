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
 * Abstract class that implements a flow graph node
 * 
 * @author Tiago
 *
 */
public abstract class FlowNode implements ToDot {
    protected ArrayList<FlowEdge> inEdges = new ArrayList<>();
    protected ArrayList<FlowEdge> outEdges = new ArrayList<>();
    protected int id;
    protected String name;
    protected String label;
    private boolean disabled = false;
    private boolean explored = false;

    public FlowNode(String label) {
	this.label = label;
    }

    /**
     * Initializes the node with a prefix and an ID
     * 
     * @param prefix
     * @param id
     */
    public void initNode(String prefix, int id) {
	this.id = id;
	this.name = prefix + id;
    }

    /**
     * Adds an incoming edge to the adjacency list
     * 
     * @param e
     */
    public void addInEdge(FlowEdge e) {
	inEdges.add(e);
    }

    /**
     * Adds an outcoming edge to the adjacency list
     * 
     * @param e
     */
    public void addOutEdge(FlowEdge e) {
	outEdges.add(e);
    }

    /**
     * Removes an incoming edge from the adjacency list
     * 
     * @param e
     */
    public void removeInEdge(FlowEdge e) {
	inEdges.remove(e);
    }

    /**
     * Removes an outcoming edge from the adjacency list
     * 
     * @param e
     */
    public void removeOutEdge(FlowEdge e) {
	outEdges.remove(e);
    }

    /**
     * Gets all incoming nodes
     * 
     * @return
     */
    public ArrayList<FlowNode> getInNodes() {
	ArrayList<FlowNode> nodes = new ArrayList<>();

	for (FlowEdge e : this.inEdges)
	    nodes.add(e.getSource());
	return nodes;
    }

    /**
     * Gets all outgoing nodes
     * 
     * @return
     */
    public ArrayList<FlowNode> getOutNodes() {
	ArrayList<FlowNode> nodes = new ArrayList<>();

	for (FlowEdge e : this.outEdges)
	    nodes.add(e.getDest());
	return nodes;
    }

    /**
     * Sets a new label for the node
     * 
     * @param label
     */
    public void setLabel(String label) {
	this.label = label;
    }

    /**
     * Gets the node label
     * 
     * @return
     */
    public String getLabel() {
	return label;
    }

    /**
     * Gets the node ID
     * 
     * @return
     */
    public int getId() {
	return id;
    }

    /**
     * Gets the node name
     * 
     * @return
     */
    public String getName() {
	return name;
    }

    /**
     * Checks whether the node is disabled
     * 
     * @return
     */
    public boolean isDisabled() {
	return disabled;
    }

    /**
     * Sets whether the node is disabled
     * 
     * @param disabled
     */
    public void setDisabled(boolean disabled) {
	this.disabled = disabled;
    }

    /**
     * Gets all outgoing edges
     * 
     * @return
     */
    public ArrayList<FlowEdge> getOutEdges() {
	return outEdges;
    }

    /**
     * Gets all incoming edges
     * 
     * @return
     */
    public ArrayList<FlowEdge> getInEdges() {
	return inEdges;
    }

    /**
     * Clears the entire adjacency list
     */
    public void clear() {
	this.inEdges.clear();
	this.outEdges.clear();
    }

    /**
     * Checks whether the node was already explored. Useful for graph-based
     * algorithms such as DFS or Dijkstra's Shortest Path
     * 
     * @return
     */
    public boolean isExplored() {
	return explored;
    }

    /**
     * Sets the node as being explored
     * 
     * @param explored
     */
    public void setExplored(boolean explored) {
	this.explored = explored;
    }

    @Override
    public boolean equals(Object obj) {
	return id == ((FlowNode) obj).getId();
    }
}
