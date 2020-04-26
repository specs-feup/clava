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

import pt.up.fe.specs.clava.analysis.flow.FlowNode;

public class DataFlowNode extends FlowNode implements Cloneable {

    private DataFlowNodeType type;
    private int nIterations = 0;
    private int subgraphID = -1;
    private boolean isSubgraphRoot = false;

    public DataFlowNode(DataFlowNodeType type, String label) {
	super(label);
	this.type = type;
    }

    @Override
    public String toDot() {
	StringBuilder sb = new StringBuilder();
	sb.append(name).append(" [label=\"").append(label).append("\" color=\"").append(type.getColor()).append("\"")
		.append("]");
	return sb.toString();
    }

    @Override
    public String toString() {
	return name + "  [" + label + "]";
    }

    public void setIterations(int numIter) {
	nIterations = numIter;
    }

    public int getIterations() {
	return nIterations;
    }

    public DataFlowNodeType getType() {
	return type;
    }

    public void setType(DataFlowNodeType type) {
	this.type = type;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
	DataFlowNode node = new DataFlowNode(this.type, this.label);
	return node;
    }

    public int getSubgraphID() {
	return subgraphID;
    }

    public void setSubgraphID(int subgraphID) {
	this.subgraphID = subgraphID;
    }

    public boolean isSubgraphRoot() {
	return isSubgraphRoot;
    }

    public void setSubgraphRoot(boolean isSubgraphRoot) {
	this.isSubgraphRoot = isSubgraphRoot;
    }
}
