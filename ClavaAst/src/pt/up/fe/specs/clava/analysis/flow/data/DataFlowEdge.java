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

import pt.up.fe.specs.clava.analysis.flow.FlowEdge;
import pt.up.fe.specs.clava.analysis.flow.FlowNode;

public class DataFlowEdge extends FlowEdge {
    public int repeating = 0;
    public boolean directed = true;
    private DataFlowEdgeType type = DataFlowEdgeType.DATAFLOW;

    /**
     * Constructor for directed edge of default type "data"
     * 
     * @param source
     * @param dest
     */
    public DataFlowEdge(FlowNode source, FlowNode dest) {
	super(source, dest);
    }

    /**
     * Constructor for directed edge of provided type
     * 
     * @param source
     * @param dest
     * @param type
     */
    public DataFlowEdge(FlowNode source, FlowNode dest, DataFlowEdgeType type) {
	super(source, dest);
	this.type = type;
	if (type == DataFlowEdgeType.CONTROL_REPEATING)
	    this.directed = false;
    }

    /**
     * Constructor for directed edge of type "repeating", with provided number of
     * repetitions
     * 
     * @param source
     * @param dest
     * @param repeating
     */
    public DataFlowEdge(FlowNode source, FlowNode dest, int repeating) {
	super(source, dest);
	this.repeating = repeating;
	// this.directed = false;
	this.type = DataFlowEdgeType.CONTROL_REPEATING;
    }

    /**
     * Constructor for edge with provided type and direction
     * 
     * @param source
     * @param dest
     * @param type
     * @param directed
     */
    public DataFlowEdge(FlowNode source, FlowNode dest, DataFlowEdgeType type, boolean directed) {
	super(source, dest);
	this.directed = directed;
    }

    @Override
    public String toDot() {
	String sourceLabel = "n" + getSource().getId();
	String destLabel = "n" + getDest().getId();
	StringBuilder sb = new StringBuilder();
	sb.append(sourceLabel).append(" -> ").append(destLabel).append(" [label=\"");
	if (repeating > 0)
	    sb.append("x" + repeating);
	sb.append("\", color=").append(type.getColor());
//	if (!directed) {
//	    sb.append(", dir=none");
//	}
	sb.append("]");
	return sb.toString();
    }

    public DataFlowEdgeType getType() {
	return type;
    }

    public void setType(DataFlowEdgeType type) {
	this.type = type;
    }
}
