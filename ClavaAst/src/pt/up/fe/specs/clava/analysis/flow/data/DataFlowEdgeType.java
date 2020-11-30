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

/**
 * Enumeration for the DFG edge types Black: normal dataflow edge; Red: index
 * dataflow edge; Blue: repetitions (usually used for loops); Orange: dependency
 * tracking (experimental)
 * 
 * @author Tiago
 *
 */
public enum DataFlowEdgeType {
    DATAFLOW("black"), DATAFLOW_INDEX("red"), REPEATING("blue"), DEPENDENCY("orange");

    private String color;

    private DataFlowEdgeType(String color) {
	this.color = color;
    }

    /**
     * Gets the color of the edge for display purposes
     * 
     * @return edge color using DOT language semantics
     */
    public String getColor() {
	return color;
    }

    /**
     * Checks if an edge is used for control purposes (by definition, only the
     * REPEATING edges are)
     * 
     * @param type
     * @return
     */
    public static boolean isControl(DataFlowEdgeType type) {
	return type == REPEATING;
    }

    @Deprecated
    private static boolean isDirected(DataFlowEdgeType type) {
	return type == DATAFLOW || type == DATAFLOW_INDEX || type == REPEATING;
    }
}
