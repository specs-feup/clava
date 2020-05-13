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

public enum DataFlowNodeType {
    LOOP("blue1"), OP_ARITH("darkorchid2"), OP_CALL("darkorchid2"), LOAD_VAR("darkgreen"), LOAD_ARRAY("darkgreen"),
    LOAD_INDEX("red"), STORE_VAR("goldenrod2"), STORE_ARRAY("goldenrod2"), CONSTANT("gray36"), TEMP("gray36"),
    NULL("white");

    private String color;

    private DataFlowNodeType(String color) {
	this.color = color;
    }

    public String getColor() {
	return color;
    }

    public static boolean isLoad(DataFlowNodeType type) {
	return type == LOAD_VAR || type == LOAD_ARRAY || type == LOAD_INDEX;
    }

    public static boolean isStore(DataFlowNodeType type) {
	return type == STORE_VAR || type == STORE_ARRAY;
    }

    public static boolean isOp(DataFlowNodeType type) {
	return type == OP_ARITH || type == OP_CALL;
    }

    public static boolean isLoop(DataFlowNodeType type) {
	return type == LOOP;
    }
}
