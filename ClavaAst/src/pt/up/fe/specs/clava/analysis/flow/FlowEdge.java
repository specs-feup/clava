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

public abstract class FlowEdge implements ToDot {
    private FlowNode source;
    private FlowNode dest;
    private String label;

    public FlowEdge(FlowNode source, FlowNode dest) {
	this.source = source;
	this.dest = dest;
    }

    public String getLabel() {
	return label;
    }

    public void setLabel(String label) {
	this.label = label;
    }

    public FlowNode getSource() {
	return source;
    }

    public void setSource(FlowNode source) {
	this.source = source;
    }

    public FlowNode getDest() {
	return dest;
    }

    public void setDest(FlowNode dest) {
	this.dest = dest;
    }

}
