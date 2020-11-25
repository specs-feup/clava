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

/**
 * Data class that holds information about the features of a subgraph. This
 * class does not reason about the subgraph; it only holds information that
 * needs to be precalculated and filled in method by method
 * 
 * @author Tiago
 *
 */
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
    private long iterations = 1;
    public static String HEADER = "ID,Statement,Iterations,LoadsVar,LoadsArr,StoresVar,StoresArr,Ops,Calls,Depth,Critical Path";

    /**
     * Constructor, takes the root of a subgraph
     * 
     * @param root
     */
    public DataFlowSubgraphMetrics(DataFlowNode root) {
	this.root = root;
	this.id = root.getSubgraphID();
    }

    /**
     * Gets the root of the subgraph
     * 
     * @return
     */
    public DataFlowNode getRoot() {
	return root;
    }

    /**
     * Gets the number of operations in the subgraph
     * 
     * @return
     */
    public int getNumOp() {
	return numOp;
    }

    /**
     * Sets the number of operations of the subgraph
     * 
     * @param numOp
     */
    public void setNumOp(int numOp) {
	this.numOp = numOp;
    }

    /**
     * Gets the depth of the subgraph
     * 
     * @return
     */
    public int getDepth() {
	return depth;
    }

    /**
     * Sets the depth of the subgraph
     * 
     * @param depth
     */
    public void setDepth(int depth) {
	this.depth = depth;
    }

    /**
     * Gets the critical path (path with the largest number of operations) of the
     * subgraph
     * 
     * @return
     */
    public ArrayList<DataFlowNode> getCriticalPath() {
	return criticalPath;
    }

    /**
     * Gets the critical path as a ready-to-print string
     * 
     * @return
     */
    public String getCriticalPathString() {
	ArrayList<String> labels = new ArrayList<>();
	for (DataFlowNode node : criticalPath)
	    labels.add(node.getLabel());
	return String.join("->", labels);
    }

    /**
     * Sets the critical path of the subgraph
     * 
     * @param criticalPath
     */
    public void setCriticalPath(ArrayList<DataFlowNode> criticalPath) {
	Collections.reverse(criticalPath);
	this.criticalPath = criticalPath;
    }

    /**
     * Gets the number of variable loads of the subgraph
     * 
     * @return
     */
    public int getNumVarLoads() {
	return numVarLoads;
    }

    /**
     * Sets the number of variable loads of the subgraph
     * 
     * @param n
     */
    public void setNumVarLoads(int n) {
	this.numVarLoads = n;
    }

    /**
     * Gets the number of variable stores of the subgraph
     * 
     * @return
     */
    public int getNumVarStores() {
	return numVarStores;
    }

    /**
     * Sets the number of variable stores of the subgraph
     * 
     * @param n
     */
    public void setNumVarStores(int n) {
	this.numVarStores = n;
    }

    /**
     * Gets the C code string that matches the subgraph
     * 
     * @return
     */
    public String getCode() {
	return code;
    }

    /**
     * Sets the C code string that matches the subgraph
     * 
     * @param code
     */
    public void setCode(String code) {
	this.code = code;
    }

    /**
     * Sets the number of iterations of the subgraph
     * 
     * @param l
     */
    public void setIterations(long l) {
	this.iterations = l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
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

    /**
     * Gets the number of iterations of the subgraph
     * 
     * @return
     */
    public long getIterations() {
	return iterations;
    }

    /**
     * Gets the number of calls of the subgraph
     * 
     * @return
     */
    public int getNumCalls() {
	return numCalls;
    }

    /**
     * Sets the number of calls of the subgraph
     * 
     * @param numCalls
     */
    public void setNumCalls(int numCalls) {
	this.numCalls = numCalls;
    }

    /**
     * Gets the ID of the subgraph
     * 
     * @return
     */
    public int getId() {
	return id;
    }

    /**
     * Sets the ID of the subgraph
     * 
     * @param id
     */
    public void setId(int id) {
	this.id = id;
    }

    /**
     * Gets the number of array loads of the subgraph
     * 
     * @return
     */
    public int getNumArrayLoads() {
	return numArrayLoads;
    }

    /**
     * Sets the number of array loads of the subgraph
     * 
     * @param numArrayLoads
     */
    public void setNumArrayLoads(int numArrayLoads) {
	this.numArrayLoads = numArrayLoads;
    }

    /**
     * Gets the number of array stores of the subgraph
     * 
     * @return
     */
    public int getNumArrayStores() {
	return numArrayStores;
    }

    /**
     * Sets the number of array stores of the subgraph
     * 
     * @param numArrayStores
     */
    public void setNumArrayStores(int numArrayStores) {
	this.numArrayStores = numArrayStores;
    }
}
