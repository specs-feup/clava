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

public class HLSStream extends HLSDirective {
    private String variable;
    private int depth = -1;
    private int dim = -1;
    private boolean isOff = false;

    public HLSStream(String variable) {
	this.variable = variable;
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append(super.toString()).append("stream variable=").append(variable);
	if (depth != -1)
	    sb.append(" depth=").append(depth);
	if (dim != -1)
	    sb.append(" dim=").append(dim);
	if (isOff)
	    sb.append(" off");
	return sb.toString();
    }

    public void setDepth(int depth) {
	this.depth = depth;
    }

    public void setDim(int dim) {
	this.dim = dim;
    }

    public void setOff(boolean isOff) {
	this.isOff = isOff;
    }
}
