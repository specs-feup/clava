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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.analysis.flow.FlowNode;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

/**
 * Class that represents a DFG node
 * 
 * @author Tiago
 *
 */
public class DataFlowNode extends FlowNode implements Cloneable {

    /**
     * Enumeration to tell whether the node should be selected on a conditional
     * multiplexer (e.g., if the node depends on an if-else). NONE is the default
     * for nodes outside of conditions
     * 
     * @author Tiago
     *
     */
    public enum BooleanSelector {
	NONE, TRUE, FALSE;
    }

    private DataFlowNodeType type;
    private long nIterations = 0;
    private int subgraphID = -1;
    private boolean isSubgraphRoot = false;
    private boolean isTopLevel = false;
    private Stmt stmt;
    private ArrayList<DataFlowNode> currPath = new ArrayList<>();
    private ClavaNode clavaNode;
    private BooleanSelector selector = BooleanSelector.NONE;
    private String shape = "circle";

    public DataFlowNode(DataFlowNodeType type, String label, ClavaNode node) {
	super(label);
	this.type = type;
	this.clavaNode = node;
	this.stmt = findStmt(node);
    }

    @Override
    public String toDot() {
	StringBuilder sb = new StringBuilder();
	sb.append(name).append(" [label=\"").append(label).append("\" color=\"").append(type.getColor()).append("\"");
	if (!shape.equals("circle"))
	    sb.append("shape=\"").append(shape).append("\"");
	sb.append("]");
	return sb.toString();
    }

    /**
     * Sets the number of iterations of the node
     * 
     * @param numIter
     */
    public void setIterations(long numIter) {
	nIterations = numIter;
    }

    /**
     * Gets the number of iterations of the node
     * 
     * @return the number of iterations
     */
    public long getIterations() {
	return nIterations;
    }

    /**
     * Gets the type of the node
     * 
     * @return the node type
     */
    public DataFlowNodeType getType() {
	return type;
    }

    /**
     * Sets the type of the node
     * 
     * @param type
     */
    public void setType(DataFlowNodeType type) {
	this.type = type;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
	DataFlowNode node = new DataFlowNode(this.type, this.label, null);
	return node;
    }

    /**
     * Gets the ID of the subgraph this node belongs to
     * 
     * @return
     */
    public int getSubgraphID() {
	return subgraphID;
    }

    /**
     * Sets the ID of the subgraph this node belongs to
     * 
     * @param subgraphID
     */
    public void setSubgraphID(int subgraphID) {
	this.subgraphID = subgraphID;
    }

    /**
     * Checks whether this node is a subgraph root
     * 
     * @return
     */
    public boolean isSubgraphRoot() {
	return isSubgraphRoot;
    }

    /**
     * Sets this node as being a subgraph root
     * 
     * @param isSubgraphRoot
     */
    public void setSubgraphRoot(boolean isSubgraphRoot) {
	this.isSubgraphRoot = isSubgraphRoot;
    }

    /**
     * Gets the current path of this node. Used for critical path calculation
     * 
     * @return
     */
    public ArrayList<DataFlowNode> getCurrPath() {
	return currPath;
    }

    /**
     * Gets the C code statement that better matches this node
     * 
     * @return
     */
    public Stmt getStmt() {
	return stmt;
    }

    /**
     * Sets the C code statement that better matches this node
     * 
     * @param stmt
     */
    public void setStmt(Stmt stmt) {
	this.stmt = stmt;
    }

    /**
     * Finds the C code statement that better matches this code
     * 
     * @param node
     * @return
     */
    private Stmt findStmt(ClavaNode node) {
	return (node != null) ? node.getAncestor(Stmt.class) : null;
    }

    /**
     * Gets the Clava node used to create this node
     * 
     * @return
     */
    public ClavaNode getClavaNode() {
	return clavaNode;
    }

    /**
     * Sets the Clava node used to create this node
     * 
     * @param clavaNode
     */
    public void setClavaNode(ClavaNode clavaNode) {
	this.clavaNode = clavaNode;
    }

    /**
     * Checks whether this node is in the function top level
     * 
     * @return
     */
    public boolean isTopLevel() {
	return isTopLevel;
    }

    /**
     * Sets whether this node is in the function top level
     * 
     * @param isTopLevel
     */
    public void setTopLevel(boolean isTopLevel) {
	this.isTopLevel = isTopLevel;
    }

    @Override
    public String toString() {
	return "DataFlowNode [type=" + type + ", nIterations=" + nIterations + ", subgraphID=" + subgraphID
		+ ", isSubgraphRoot=" + isSubgraphRoot + ", isTopLevel=" + isTopLevel + ", id=" + id + ", name=" + name
		+ ", label=" + label + "]";
    }

    /**
     * Gets the boolean selector of this node
     * 
     * @return
     */
    public BooleanSelector getSelector() {
	return selector;
    }

    /**
     * Sets the boolean selector of this node
     * 
     * @param selector
     */
    public void setSelector(BooleanSelector selector) {
	this.selector = selector;
    }

    /**
     * Gets the shape of this node in the DOT language (used for display purposes)
     * 
     * @return
     */
    public String getShape() {
	return shape;
    }

    /**
     * Sets the shape of this node in the DOT language (used for display purposes)
     * 
     * @param shape
     */
    public void setShape(String shape) {
	this.shape = shape;
    }
}
