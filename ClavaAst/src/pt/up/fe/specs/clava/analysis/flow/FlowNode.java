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

    public void initNode(String prefix, int id) {
	this.id = id;
	this.name = prefix + id;
    }

    public void addInEdge(FlowEdge e) {
	inEdges.add(e);
    }

    public void addOutEdge(FlowEdge e) {
	outEdges.add(e);
    }

    public void removeInEdge(FlowEdge e) {
	inEdges.remove(e);
    }

    public void removeOutEdge(FlowEdge e) {
	outEdges.remove(e);
    }

    public ArrayList<FlowNode> getInNodes() {
	ArrayList<FlowNode> nodes = new ArrayList<>();

	for (FlowEdge e : this.inEdges)
	    nodes.add(e.getSource());
	return nodes;
    }

    public ArrayList<FlowNode> getOutNodes() {
	ArrayList<FlowNode> nodes = new ArrayList<>();

	for (FlowEdge e : this.outEdges)
	    nodes.add(e.getDest());
	return nodes;
    }

    public void setLabel(String label) {
	this.label = label;
    }

    public String getLabel() {
	return label;
    }

    public int getId() {
	return id;
    }

    public String getName() {
	return name;
    }

    public boolean isDisabled() {
	return disabled;
    }

    public void setDisabled(boolean disabled) {
	this.disabled = disabled;
    }

    public ArrayList<FlowEdge> getOutEdges() {
	return outEdges;
    }

    public ArrayList<FlowEdge> getInEdges() {
	return inEdges;
    }

    public void clear() {
	this.inEdges.clear();
	this.outEdges.clear();
    }

    public boolean isExplored() {
	return explored;
    }

    public void setExplored(boolean explored) {
	this.explored = explored;
    }

    @Override
    public boolean equals(Object obj) {
	return id == ((FlowNode) obj).getId();
    }
}
