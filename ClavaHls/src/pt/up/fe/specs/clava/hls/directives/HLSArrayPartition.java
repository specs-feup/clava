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

package pt.up.fe.specs.clava.hls.directives;

public class HLSArrayPartition extends HLSDirective {
    private PartitionType type;
    private String variable;
    private int factor;
    private int dim = -1;

    public enum PartitionType {
	CYCLIC, BLOCK, COMPLETE
    }

    public HLSArrayPartition(PartitionType type, String variable, int factor) {
	this.type = type;
	this.variable = variable;
	this.factor = factor;
    }

    public void setDim(int dim) {
	this.dim = dim;
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder(super.toString()).append("array_partition variable=").append(variable)
		.append(" ").append(type.toString().toLowerCase()).append(" factor=").append(factor);
	if (dim != -1)
	    sb.append(" dim=").append(dim);
	return sb.toString();
    }
}
