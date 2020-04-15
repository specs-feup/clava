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
    LOOP("#d11515"), OP_ARITH("#5c420a"), OP_CALL("#5c420a"), LOAD_VAR("#10611c"), LOAD_ARRAY("#10611c"),
    LOAD_INDEX("#197dcf"), STORE_VAR("#e37419"), STORE_ARRAY("#e37419"), CONSTANT("#555555"), TEMP("#000000"),
    NULL("#ffffff");

    private String color;

    private DataFlowNodeType(String color) {
	this.color = color;
    }

    public String getColor() {
	return color;
    }
}
