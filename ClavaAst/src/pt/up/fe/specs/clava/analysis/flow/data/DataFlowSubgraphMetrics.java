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
    private int id = -1;
    private int numOp = -1;
    private int numCalls = -1;
    private int numVarLoads = -1;
    private int numVarStores = -1;
    private int numArrayLoads = -1;
    private int numArrayStores = -1;
    private int depth = -1;
    private ArrayList<DataFlowNode> criticalPath = new ArrayList<>();
    private String code;
    private int iterations = 1;
    public static String HEADER = "ID,Statement,Iterations,LoadsVar,LoadsArr,StoresVar,StoresArr,Ops,Calls,Depth,Critical Path";

    public DataFlowSubgraphMetrics(DataFlowNode root) {
	this.root = root;
	this.id = root.getSubgraphID();
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

    public String getCriticalPathString() {
	ArrayList<String> labels = new ArrayList<>();
	for (DataFlowNode node : criticalPath)
	    labels.add(node.getLabel());
	return String.join("->", labels);
    }

    public void setCriticalPath(ArrayList<DataFlowNode> criticalPath) {
	Collections.reverse(criticalPath);
	this.criticalPath = criticalPath;
    }

    public int getNumVarLoads() {
	return numVarLoads;
    }

    public void setNumVarLoads(int n) {
	this.numVarLoads = n;
    }

    public int getNumVarStores() {
	return numVarStores;
    }

    public void setNumVarStores(int n) {
	this.numVarStores = n;
    }

    public String getCode() {
	return code;
    }

    public void setCode(String code) {
	this.code = code;
    }

    public void setIterations(int freq) {
	this.iterations = freq;
    }

    @Override
    public String toString() {
	String CM = ",";
	StringBuilder sb = new StringBuilder();
	sb.append(id).append(CM).append(root.getStmt().getCode().replace(";", "")).append(CM).append(iterations)
		.append(CM).append(numVarLoads).append(CM).append(numArrayLoads).append(CM).append(numVarStores)
		.append(CM).append(numArrayStores).append(CM).append(numOp).append(CM).append(numCalls).append(CM)
		.append(depth).append(CM).append(getCriticalPathString());
	return sb.toString();
    }

    public int getIterations() {
	return iterations;
    }

    public int getNumCalls() {
	return numCalls;
    }

    public void setNumCalls(int numCalls) {
	this.numCalls = numCalls;
    }

    public int getId() {
	return id;
    }

    public void setId(int id) {
	this.id = id;
    }

    public int getNumArrayLoads() {
	return numArrayLoads;
    }

    public void setNumArrayLoads(int numArrayLoads) {
	this.numArrayLoads = numArrayLoads;
    }

    public int getNumArrayStores() {
	return numArrayStores;
    }

    public void setNumArrayStores(int numArrayStores) {
	this.numArrayStores = numArrayStores;
    }
}
