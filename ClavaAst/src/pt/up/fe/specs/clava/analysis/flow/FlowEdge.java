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

/**
 * Abstract class that represents an edge of a generic flow graph (which, by
 * definition, is a directed edge)
 * 
 * @author Tiago
 *
 */
public abstract class FlowEdge implements ToDot {
    private FlowNode source;
    private FlowNode dest;
    private String label;

    public FlowEdge(FlowNode source, FlowNode dest) {
	this.source = source;
	this.dest = dest;
    }

    /**
     * Gets the label of the edge
     * 
     * @return
     */
    public String getLabel() {
	return label;
    }

    /**
     * Sets the label of the edge
     * 
     * @param label
     */
    public void setLabel(String label) {
	this.label = label;
    }

    /**
     * Gets the source of the edge
     * 
     * @return
     */
    public FlowNode getSource() {
	return source;
    }

    /**
     * Sets the source of the edge
     * 
     * @param source
     */
    public void setSource(FlowNode source) {
	this.source = source;
    }

    /**
     * Gets the destination of the edge
     * 
     * @return
     */
    public FlowNode getDest() {
	return dest;
    }

    /**
     * Sets the destination of the dge
     * 
     * @param dest
     */
    public void setDest(FlowNode dest) {
	this.dest = dest;
    }

}
