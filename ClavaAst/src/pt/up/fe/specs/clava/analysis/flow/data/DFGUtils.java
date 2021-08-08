/**
 * 
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
import java.util.Stack;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.analysis.flow.FlowEdge;
import pt.up.fe.specs.clava.analysis.flow.FlowNode;

/**
 * Static class with methods to manipulate a DFG
 * 
 * @author Tiago
 *
 */
public class DFGUtils {
    /**
     * Finds the loop node that immediately precedes the given node
     * 
     * @param node
     * @return The parent loop node closest to the given node if it exists; retursn the node itself otherwise
     */
    public static DataFlowNode getLoopOfNode(DataFlowNode node) {
        if (DataFlowNodeType.isStore(node.getType())) {
            return getLoopOfLoop(node);
        }
        if (DataFlowNodeType.isLoop(node.getType())) {
            return getLoopOfStore(node);
        }
        if (DataFlowNodeType.isLoad(node.getType()) || DataFlowNodeType.isOp(node.getType())) {
            // TODO: Case for flows with no stores (e.g. calls)

            if (node.getType() == DataFlowNodeType.OP_CALL) {
                if (node.getOutEdges().size() == 0)
                    return node;
            }
            return getLoopOfStore(getStoreOfNode(node));
        }
        return node;
    }

    /**
     * Gets the Loop node that immediately precedes a given Loop node
     * 
     * @param node
     * @return the loop node that immediately precedes the node if it exists; returns the node itself otherwise
     */
    public static DataFlowNode getLoopOfLoop(DataFlowNode node) {
        for (FlowNode n : node.getInNodes()) {
            DataFlowNode curr = (DataFlowNode) n;
            if (curr.getType() == DataFlowNodeType.LOOP)
                return curr;
        }
        return node;
    }

    /**
     * Gets the loop node that immediately precedes a Store node
     * 
     * @param node
     * @return the loop node that immediately precedes the node if it exists; returns the node itself otherwise
     */
    public static DataFlowNode getLoopOfStore(DataFlowNode node) {
        for (FlowNode n : node.getInNodes()) {
            DataFlowNode curr = (DataFlowNode) n;
            if (curr.getType() == DataFlowNodeType.LOOP)
                return curr;
        }
        return node;
    }

    /**
     * Gets the closest Store node of a given node
     * 
     * @param node
     * @return the Store node that immediately precedes the node if it exists; returns the node itself otherwise
     */
    public static DataFlowNode getStoreOfNode(DataFlowNode node) {
        while (!DataFlowNodeType.isStore(node.getType())) {
            if (node.getOutNodes().size() > 0)
                node = (DataFlowNode) node.getOutNodes().get(0);
            else
                break;
        }
        return node;
    }

    /**
     * Gets a list with all the nodes that form up an index of an array, with as many nodes as the dimension of the
     * array access (e.g., arr[i][j] would return two nodes, i and j) If the index is made of a large expression, it
     * returns the closest node of that expression (e.g., a[i+1*2] would return the node that corresponds to the
     * operation "+")
     * 
     * @param node
     * @return A list with nodes that form the loop index.
     */
    public static ArrayList<DataFlowNode> getIndexesOfArray(DataFlowNode node) {
        ArrayList<DataFlowNode> nodes = new ArrayList<>();
        for (FlowEdge e : node.getInEdges()) {
            DataFlowEdge edge = (DataFlowEdge) e;
            if (edge.getType() == DataFlowEdgeType.DATAFLOW_INDEX)
                nodes.add((DataFlowNode) edge.getSource());
        }
        return nodes;
    }

    /**
     * Gets all nodes of a given type present on the provided DFG
     * 
     * @param dfg
     * @param type
     * @return a list of nodes of the given type
     */
    public static ArrayList<DataFlowNode> getAllNodesOfType(DataFlowGraph dfg, DataFlowNodeType type) {
        ArrayList<DataFlowNode> nodes = new ArrayList<>();
        for (FlowNode n : dfg.getNodes()) {
            DataFlowNode node = (DataFlowNode) n;
            if (node.getType() == type)
                nodes.add(node);
        }
        nodes = (ArrayList<DataFlowNode>) nodes.stream().distinct().collect(Collectors.toList());
        return nodes;
    }

    /**
     * Attempts to estimate the number of iterations of a given node. Only guaranteed to work if the function has a
     * predictable number of iterations on each loop in compile-time
     * 
     * @param node
     * @return the number of iterations
     */
    public static long estimateNodeFrequency(DataFlowNode node) {
        long count = node.getIterations() == 0 ? 1 : node.getIterations();
        if (node.isTopLevel())
            return count;
        DataFlowNode loop = getLoopOfNode(node);
        count *= loop.getIterations();
        while (!loop.isTopLevel()) {
            DataFlowNode previousLoop = loop;
            loop = getLoopOfLoop(loop);

            // To avoid an infinite loop
            if (previousLoop == loop) {
                break;
            }

            count *= loop.getIterations();
            count = count > Integer.MAX_VALUE ? Integer.MAX_VALUE : count;
        }
        return count == 0 ? 1 : count;
    }

    /**
     * Gets the total number of array loads in the function's runtime Uses a list of subgraph metrics as a starting
     * point
     * 
     * @param metrics
     * @return the total number of array loads
     */
    public static int sumArrayLoads(ArrayList<DataFlowSubgraphMetrics> metrics) {
        int sum = 0;
        for (DataFlowSubgraphMetrics m : metrics)
            sum += m.getNumArrayLoads() * m.getIterations();
        return sum;
    }

    /**
     * Gets all variable loads present in a subgraph
     * 
     * @param sub
     * @return a list of variable loads
     */
    public static ArrayList<DataFlowNode> getVarLoadsOfSubgraph(DataFlowSubgraph sub) {
        ArrayList<DataFlowNode> nodes = new ArrayList<>();
        for (DataFlowNode node : sub.getNodes()) {
            if (node.getType() == DataFlowNodeType.LOAD_VAR)
                nodes.add(node);
        }
        return nodes;
    }

    /**
     * Checks if a variable load is also an index load (e.g., the "i" in a[i])
     * 
     * @param node
     * @return
     */
    public static boolean isIndex(DataFlowNode node) {
        String name = node.getLabel();
        DataFlowNode loop = getLoopOfNode(node);
        if (loop == node)
            return false;
        while (loop != node) {
            if (getIteratorOfLoop(loop).equals(name))
                return true;
            else {
                node = loop;
                loop = getLoopOfLoop(node);
            }
        }
        return false;
    }

    /**
     * Gets the name of the variable used as loop iterator. Only works for loops with a single iterator
     * 
     * @param loop
     * @return the name of the loop iterator
     */
    public static String getIteratorOfLoop(DataFlowNode loop) {
        String tokens[] = loop.getLabel().split(" ");
        return tokens.length == 2 ? tokens[1] : "__undefined";
    }

    /**
     * Compares two expressions to see if they refer to the same loop access (e.g., a[i+1] and a[1+i] have different yet
     * equivalent expressions)
     * 
     * @param n1
     * @param n2
     * @return true if they are the same, false otherwise
     */
    public static boolean isSameArrayAccess(DataFlowNode n1, DataFlowNode n2) {
        ArrayList<DataFlowNode> f1 = getIndexExpr(n1);
        ArrayList<DataFlowNode> f2 = getIndexExpr(n2);
        return compareFlows(f1, f2);
    }

    /**
     * Gets the expression that forms up a loop index given the first node of the expression (e.g., given node "+" from
     * a[i+1*2], we'd fetch nodes "i", "1", "*" and "2")
     * 
     * @param n
     * @return a list of all the nodes from the expression
     */
    public static ArrayList<DataFlowNode> getIndexExpr(DataFlowNode n) {
        ArrayList<DataFlowNode> nodes = new ArrayList<>();
        Stack<DataFlowNode> s = new Stack<>();
        s.add(n);
        while (!s.isEmpty()) {
            DataFlowNode node = s.pop();
            if (!nodes.contains(node)) {
                nodes.add(node);
                for (FlowNode child : node.getInNodes()) {
                    s.add((DataFlowNode) child);
                }
            }
        }
        return nodes;
    }

    /**
     * Compares whether two data flows are equivalent
     * 
     * @param f1
     * @param f2
     * @return true if they are equivalent, false otherwise
     */
    public static boolean compareFlows(ArrayList<DataFlowNode> f1, ArrayList<DataFlowNode> f2) {
        if (f1.size() != f2.size())
            return false;
        for (int i = 0; i < f1.size(); i++) {
            if (f1.get(i).getLabel() != f2.get(i).getLabel())
                return false;
        }
        return true;
    }

    /**
     * Gets a function parameter given its name
     * 
     * @param dfg
     * @param variable
     * @return the parameter if it exists, null otherwise
     */
    public static DataFlowParam getParamByName(DataFlowGraph dfg, String variable) {
        for (DataFlowParam param : dfg.getParams()) {
            if (param.getName().equals(variable))
                return param;
        }
        return null;
    }

    /**
     * Counts how many loops exist in the uppermost scope of the function
     * 
     * @param dfg
     * @return the number of uppermost loops
     */
    public static int getTopLoopCount(DataFlowGraph dfg) {
        int cnt = 0;
        for (FlowNode n : dfg.getNodes()) {
            DataFlowNode node = (DataFlowNode) n;
            if (DataFlowNodeType.isLoop(node.getType()) && node.isTopLevel())
                cnt++;
        }
        return cnt;
    }

    /**
     * Gets all subgraphs of a given loop
     * 
     * @param loop
     * @return a list with the starting node of each subgraph
     */
    public static ArrayList<DataFlowNode> getSubgraphsOfLoop(DataFlowNode loop) {
        ArrayList<DataFlowNode> nodes = new ArrayList<>();
        for (FlowNode n : loop.getOutNodes()) {
            DataFlowNode node = (DataFlowNode) n;
            if (DataFlowNodeType.isStore(node.getType()) || DataFlowNodeType.isOp(node.getType()))
                nodes.add(node);
        }
        return nodes;
    }

    @Deprecated
    public static HashMap<String, ArrayList<DataFlowNode>> mergeSubgraphs(ArrayList<DataFlowNode> subs) {
        HashMap<String, ArrayList<DataFlowNode>> toMerge = new HashMap<>();
        return toMerge;
    }

    /**
     * Finds all variables used in the function
     * 
     * @param dfg
     * @return a list with variable names
     */
    public static ArrayList<String> findAllVariables(DataFlowGraph dfg) {
        ArrayList<String> vars = new ArrayList<>();
        for (FlowNode n : dfg.getNodes()) {
            DataFlowNode node = (DataFlowNode) n;
            if (DataFlowNodeType.isLoad(node.getType()) || DataFlowNodeType.isStore(node.getType())) {
                if (!vars.contains(node.getLabel()))
                    vars.add(node.getLabel());
            }
        }
        return vars;
    }

    /*
    @Deprecated
    public static ArrayList<DataFlowNode> getAllReferences(DataFlowGraph dfg, String label) {
        ArrayList<DataFlowNode> refs = new ArrayList<>();
        for (FlowNode n : dfg.getNodes()) {
    //            DataFlowNode node = (DataFlowNode) n;
            // if (node.)
        }
        return refs;
    }
    */
}
