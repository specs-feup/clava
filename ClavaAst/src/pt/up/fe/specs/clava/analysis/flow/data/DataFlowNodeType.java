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
 * Enumeration that lists the many types a DFG node can assume, as well as
 * information about the color for display purposes
 * 
 * @author Tiago
 *
 */
public enum DataFlowNodeType {
    LOOP("blue1"), OP_ARITH("darkorchid2"), OP_COND("darkorchid2"), OP_CALL("darkorchid2"), LOAD_VAR("darkgreen"),
    LOAD_ARRAY("darkgreen"), LOAD_INDEX("red"), STORE_VAR("goldenrod2"), STORE_ARRAY("goldenrod2"), CONSTANT("gray36"),
    TEMP("gray36"), INTERFACE("#b54707"), NULL("white");

    private String color;

    private DataFlowNodeType(String color) {
	this.color = color;
    }

    /**
     * Gets the hex color of the type
     * 
     * @return
     */
    public String getColor() {
	return color;
    }

    /**
     * Checks whether the type is a load operation
     * 
     * @param type
     * @return
     */
    public static boolean isLoad(DataFlowNodeType type) {
	return type == LOAD_VAR || type == LOAD_ARRAY || type == LOAD_INDEX;
    }

    /**
     * Checks whether the type is a store operation
     * 
     * @param type
     * @return
     */
    public static boolean isStore(DataFlowNodeType type) {
	return type == STORE_VAR || type == STORE_ARRAY;
    }

    /**
     * Checks whether the type is an arithmetic/conditional operation or a function
     * call
     * 
     * @param type
     * @return
     */
    public static boolean isOp(DataFlowNodeType type) {
	return type == OP_ARITH || type == OP_CALL || type == OP_CALL;
    }

    /**
     * Checks whether the type is a loop
     * 
     * @param type
     * @return
     */
    public static boolean isLoop(DataFlowNodeType type) {
	return type == LOOP;
    }

    /**
     * Checks whether the type is used for arrays
     * 
     * @param type
     * @return
     */
    public static boolean isArray(DataFlowNodeType type) {
	return type == LOAD_ARRAY || type == STORE_ARRAY;
    }

    /**
     * Checks if the type is used for variables
     * 
     * @param type
     * @return
     */
    public static boolean isVar(DataFlowNodeType type) {
	return type == LOAD_VAR || type == LOAD_INDEX || type == STORE_VAR;
    }
}
