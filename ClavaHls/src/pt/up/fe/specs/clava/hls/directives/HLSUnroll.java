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

public class HLSUnroll extends HLSDirective {
    private int factor = 0;
    private boolean region = false;
    private boolean skip = false;

    public void setFactor(int factor) {
	this.factor = factor;
    }

    public void setRegion(boolean region) {
	this.region = region;
    }

    public void setSkip(boolean skip) {
	this.skip = skip;
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append(super.toString()).append("unroll ");
	if (factor > 0)
	    sb.append(" factor=").append(factor);
	if (region)
	    sb.append(" region");
	if (skip)
	    sb.append(" skip_exit_check");
	return sb.toString();
    }

}
