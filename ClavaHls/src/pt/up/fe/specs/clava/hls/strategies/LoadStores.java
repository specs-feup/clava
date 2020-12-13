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

package pt.up.fe.specs.clava.hls.strategies;

import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.hls.ClavaHLS;

public class LoadStores extends RestructuringStrategy {
    private boolean isSimpleLoop = false;
    private int lsFactor;
    public static int DEFAULT_LS = 32;

    public LoadStores(DataFlowGraph dfg, int lsFactor) {
	super(dfg);
    }

    @Override
    public void analyze() {
	// TODO: the detection...
	ClavaHLS.log("checking if the function is a simple loop...");
	isSimpleLoop = true;
    }

    @Override
    public void apply() {
	if (isSimpleLoop) {
	    // TODO: Insert directives
	}
    }

}
