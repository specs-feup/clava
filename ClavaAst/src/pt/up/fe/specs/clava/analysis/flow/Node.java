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

public abstract class Node implements ToDot {
    protected ArrayList<Edge> inEdges = new ArrayList<>();
    protected ArrayList<Edge> outEdges = new ArrayList<>();
    protected static int globalID = 0;
    protected final int id;
    protected final String label;
    public boolean disabled = false;

    public Node(String label) {
	this.id = globalID;
	globalID++;
	this.label = label;
    }

    public void addInEdge(Edge e) {
	inEdges.add(e);
    }

    public void addOutEdge(Edge e) {
	outEdges.add(e);
    }

    public ArrayList<Node> getInNodes() {
	ArrayList<Node> nodes = new ArrayList<>();

	for (Edge e : this.inEdges)
	    nodes.add(e.source);
	return nodes;
    }

    public ArrayList<Node> getOutNodes() {
	ArrayList<Node> nodes = new ArrayList<>();

	for (Edge e : this.outEdges)
	    nodes.add(e.dest);
	return nodes;
    }

    public String getLabel() {
	return label;
    }

    public int getId() {
	return id;
    }
}
