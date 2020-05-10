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

public class HLSPipeline extends HLSDirective {
    private int ii = 0;
    private boolean enable_flush = false;
    private boolean rewind = false;

    public void setII(int ii) {
	this.ii = ii;
    }

    public void setEnableFlush(boolean enableFlush) {
	this.enable_flush = enableFlush;
    }

    public void setRewind(boolean rewind) {
	this.rewind = rewind;
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append(super.toString());
	sb.append(" pipeline");
	if (ii > 0)
	    sb.append(" II=").append(ii);
	if (enable_flush)
	    sb.append(" enable_flush");
	if (rewind)
	    sb.append(" rewind");
	return sb.toString();
    }
}
