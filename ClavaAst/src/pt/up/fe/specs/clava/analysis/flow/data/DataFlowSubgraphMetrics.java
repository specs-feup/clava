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

import java.util.ArrayList;
import java.util.Collections;

public class DataFlowSubgraphMetrics {
    private DataFlowNode root;
    private int numOp = -1;
    private int numLoads = -1;
    private int numStores = -1;
    private int depth = -1;
    private ArrayList<DataFlowNode> criticalPath = new ArrayList<>();

    public DataFlowSubgraphMetrics(DataFlowNode root) {
	this.root = root;
    }

    public DataFlowNode getRoot() {
	return root;
    }

    public int getNumOp() {
	return numOp;
    }

    public void setNumOp(int numOp) {
	this.numOp = numOp;
    }

    public int getDepth() {
	return depth;
    }

    public void setDepth(int depth) {
	this.depth = depth;
    }

    public ArrayList<DataFlowNode> getCriticalPath() {
	return criticalPath;
    }

    public void setCriticalPath(ArrayList<DataFlowNode> criticalPath) {
	Collections.reverse(criticalPath);
	this.criticalPath = criticalPath;
    }

    public int getNumLoads() {
	return numLoads;
    }

    public void setNumLoads(int n) {
	this.numLoads = n;
    }

    public int getNumStores() {
	return numStores;
    }

    public void setNumStores(int n) {
	this.numStores = n;
    }

    @Override
    public String toString() {
	String NL = "\n";
	StringBuilder sb = new StringBuilder();
	sb.append("Subgraph ").append(root.getId()).append(NL);
	sb.append("------------------------------").append(NL);
	sb.append("Depth: ").append(depth).append(NL);
	sb.append("Num. loads: ").append(numLoads).append(NL);
	sb.append("Num. stores: ").append(numStores).append(NL);
	sb.append("Num. operations: ").append(numOp).append(NL);
	sb.append("Critical Path:").append(NL);
	for (int i = 0; i < criticalPath.size() - 1; i++)
	    sb.append(criticalPath.get(i).getLabel()).append(" -> ");
	sb.append(criticalPath.get(criticalPath.size() - 1).getLabel()).append(NL);
	sb.append("------------------------------").append(NL);
	return sb.toString();
    }
}
