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

import pt.up.fe.specs.clava.analysis.flow.Edge;
import pt.up.fe.specs.clava.analysis.flow.Node;

public class DataFlowEdge extends Edge {
    public int repeating = 0;
    public boolean directed = true;
    public DataFlowEdgeType type = DataFlowEdgeType.DATA;

    public DataFlowEdge(Node source, Node dest) {
	super(source, dest);
    }

    public DataFlowEdge(Node source, Node dest, DataFlowEdgeType type) {
	super(source, dest);
	this.type = type;
	if (type == DataFlowEdgeType.REPEATING)
	    this.directed = false;
    }

    public DataFlowEdge(Node source, Node dest, int repeating) {
	super(source, dest);
	this.repeating = repeating;
	this.directed = false;
	this.type = DataFlowEdgeType.REPEATING;
    }

    public DataFlowEdge(Node source, Node dest, boolean directed) {
	super(source, dest);
	this.directed = directed;
    }

    @Override
    public String toDot() {
	String sourceLabel = "n" + source.getId();
	String destLabel = "n" + dest.getId();
	StringBuilder sb = new StringBuilder();
	sb.append(sourceLabel).append(" -> ").append(destLabel).append(" [label=\"");
	if (repeating > 0)
	    sb.append("x" + repeating);
	sb.append("\", color=").append(type.getColor());
	if (!directed) {
	    sb.append(", dir=none");
	}
	sb.append("]");
	return sb.toString();
    }
}
