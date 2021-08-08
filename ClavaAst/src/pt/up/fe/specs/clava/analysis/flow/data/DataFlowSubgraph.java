/**
 * Copyright 2020 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.analysis.flow.data;

import java.util.ArrayList;
import java.util.HashMap;

import pt.up.fe.specs.clava.analysis.flow.FlowNode;

/**
 * Class to represent a subgraph. A subgraph is a view over an existing DFG, and is defined as a group of nodes that
 * converge on a Store node (which serves as the root of a subgraph).
 * 
 * @author Tiago
 *
 */
public class DataFlowSubgraph {
    private DataFlowNode root;
    private int id;
    private DataFlowGraph dfg;

    public DataFlowSubgraph(DataFlowNode root, DataFlowGraph dfg) {
        this.root = root;
        this.dfg = dfg;
        this.id = root.getSubgraphID();
    }

    /**
     * Gets all the nodes of the subgraph
     * 
     * @return a list of nodes
     */
    public ArrayList<DataFlowNode> getNodes() {
        ArrayList<DataFlowNode> nodes = findNodes(root);
        nodes.forEach(node -> {
            node.setExplored(false);
        });
        return nodes;
    }

    /*
    @Deprecated
    private ArrayList<DataFlowNode> findNodesRecursively(DataFlowNode node) {
    ArrayList<DataFlowNode> nodes = new ArrayList<>();
    if (!node.isExplored()) {
        nodes.add(node);
        node.setExplored(true);
        for (FlowNode n : node.getInNodes()) {
    	DataFlowNode ascendant = (DataFlowNode) n;
    	nodes.addAll(findNodes(ascendant));
        }
    }
    return nodes;
    }
    */

    /**
     * Finds all nodes of the subgraph given the root
     * 
     * @param root
     * @return a list of nodes
     */
    private ArrayList<DataFlowNode> findNodes(DataFlowNode root) {
        ArrayList<DataFlowNode> nodes = new ArrayList<>();
        for (FlowNode n : dfg.getNodes()) {
            DataFlowNode node = (DataFlowNode) n;
            if (node.getSubgraphID() == this.id)
                nodes.add(node);
        }
        return nodes;
    }

    /**
     * Gets all var loads with more than one occurrence, e.g., the subgraph of int a = b + b + c would return the var
     * load node "b", but not of "c"
     * 
     * @return a list of nodes
     */
    public HashMap<String, ArrayList<DataFlowNode>> getMultipleVarLoads() {
        HashMap<String, ArrayList<DataFlowNode>> map = new HashMap<>();
        ArrayList<DataFlowNode> nodes = getNodes();
        for (DataFlowNode node : nodes) {
            if (node.getType() == DataFlowNodeType.LOAD_ARRAY || node.getType() == DataFlowNodeType.LOAD_VAR
                    || node.getType() == DataFlowNodeType.LOAD_INDEX) {
                if (map.containsKey(node.getLabel())) {
                    map.get(node.getLabel()).add(node);
                } else {
                    ArrayList<DataFlowNode> labelNodes = new ArrayList<>();
                    labelNodes.add(node);
                    map.put(node.getLabel(), labelNodes);
                }
            }
        }
        map.entrySet().removeIf(e -> e.getValue().size() == 1);
        return map;
    }

    /**
     * Calculates the metrics of the subgraph, encapsulating them on a data class
     * 
     * @return a data class with the metrics filled in
     */
    public DataFlowSubgraphMetrics getMetrics() {
        DataFlowSubgraphMetrics metrics = new DataFlowSubgraphMetrics(root);
        calculateCriticalPath(root);
        ArrayList<DataFlowNode> path = root.getCurrPath();
        metrics.setCriticalPath(path);
        metrics.setDepth(calculateDepth(path));

        metrics.setNumVarLoads(findLoads(false));
        metrics.setNumArrayLoads(findLoads(true));

        metrics.setNumVarStores(findStores(false));
        metrics.setNumArrayStores(findStores(true));

        metrics.setNumOp(findArithOps());
        metrics.setNumCalls(findCalls());
        metrics.setCode(root.getStmt().getCode());
        // System.out.println(this.toString());
        return metrics;
    }

    /**
     * Counts all arithmetic operations of the subgraph
     * 
     * @return the number of arithmetic operations
     */
    private int findArithOps() {
        ArrayList<DataFlowNode> nodes = this.getNodes();
        int counter = 0;
        for (DataFlowNode node : nodes) {
            if (node.getType() == DataFlowNodeType.OP_ARITH) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * Counts all function calls of the subgraph
     * 
     * @return the number of function calls
     */
    private int findCalls() {
        ArrayList<DataFlowNode> nodes = this.getNodes();
        int counter = 0;
        for (DataFlowNode node : nodes) {
            if (node.getType() == DataFlowNodeType.OP_CALL) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * Counts all stores of the subgraph. If array=false, it counts all variable stores. Otherwise, it counts all array
     * stores. Important note: by the current definition of subgraph, this should always return 1, as the store node
     * serves as the subgraph root
     * 
     * @param array
     * @return the number of stores of the specified kind
     */
    private int findStores(boolean array) {
        ArrayList<DataFlowNode> nodes = this.getNodes();
        int counter = 0;
        for (DataFlowNode node : nodes) {
            if (array) {
                if (node.getType() == DataFlowNodeType.STORE_ARRAY)
                    counter++;
            } else {
                if (node.getType() == DataFlowNodeType.STORE_VAR)
                    counter++;
            }
        }
        return counter;
    }

    /**
     * Counts all loads of a given type (false for variables, true for arrays)
     * 
     * @param array
     * @return the number of loads of the specified kind
     */
    private int findLoads(boolean array) {
        ArrayList<DataFlowNode> nodes = this.getNodes();
        int counter = 0;
        for (DataFlowNode node : nodes) {
            if (DataFlowNodeType.isLoad(node.getType())) {
                if (array) {
                    if (node.getType() == DataFlowNodeType.LOAD_ARRAY)
                        counter++;
                } else {
                    if (node.getType() == DataFlowNodeType.LOAD_VAR || node.getType() == DataFlowNodeType.LOAD_INDEX)
                        counter++;
                }
            }
        }

        if (root.getOutEdges().size() >= 1) {
            DataFlowEdge edge = (DataFlowEdge) root.getOutEdges().get(0);
            if (edge.getType() == DataFlowEdgeType.DATAFLOW) {
                if (root.getType() == DataFlowNodeType.STORE_ARRAY && array)
                    counter++;
                if (root.getType() == DataFlowNodeType.STORE_VAR && !array)
                    counter++;
            }
        }
        return counter;
    }

    /**
     * Calculates the critical path, that is, the set of nodes, ending at the root, with the largest number of
     * operations. Recursive algorithm that needs a starting node. The critical path is dynamically stored in a class
     * attribute, and this algorithm only returns the depth
     * 
     * @param node
     * @return the depth of the critical path
     */
    private int calculateCriticalPath(DataFlowNode node) {
        int count = 0;
        if (node.getType() == DataFlowNodeType.OP_ARITH || node.getType() == DataFlowNodeType.OP_CALL)
            count++;
        node.getCurrPath().add(node);

        int max = -1;
        ArrayList<DataFlowNode> ascendantPath = new ArrayList<>();
        for (FlowNode n : node.getInNodes()) {
            DataFlowNode ascendant = (DataFlowNode) n;
            if (ascendant != root) {
                int ascendantCount = calculateCriticalPath(ascendant);
                if (ascendantCount > max) {
                    max = ascendantCount;
                    ascendantPath = ascendant.getCurrPath();
                }
            }
        }
        max = (max == -1) ? 0 : max;
        node.getCurrPath().addAll(ascendantPath);
        return count + max;
    }

    /**
     * Calculates the depth of a given critical path (number of operations)
     * 
     * @param path
     * @return depth of the path
     */
    private int calculateDepth(ArrayList<DataFlowNode> path) {
        int count = 0;
        for (DataFlowNode node : path) {
            if (DataFlowNodeType.isOp(node.getType()))
                count++;
        }
        return count;
    }

    /**
     * Gets the ID of the subgraph
     * 
     * @return the subgraph ID
     */
    public int getId() {
        return id;
    }

    /**
     * Sets an ID for the subgraph. A subgraph must have a unique ID within its DFG, which must be enforced by the DFG
     * itself, and not by this class
     * 
     * @param id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the root of the subgraph. By definition, it is a store node
     * 
     * @return the root of the subgraph
     */
    public DataFlowNode getRoot() {
        return root;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Subgraph of ");
        sb.append(this.root.getStmt().getCode()).append("\n");
        for (DataFlowNode n : this.getNodes()) {
            sb.append(n.getLabel()).append(", type = ").append(n.getType()).append("\n");
        }
        return sb.toString();
    }
}
