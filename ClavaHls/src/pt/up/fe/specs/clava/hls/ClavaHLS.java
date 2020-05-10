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

package pt.up.fe.specs.clava.hls;

import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.hls.strategies.ArrayStreamDetector;
import pt.up.fe.specs.clava.hls.strategies.NestedLoopUnrolling;

public class ClavaHLS {
    private DataFlowGraph dfg;

    public ClavaHLS(DataFlowGraph dfg) {
	this.dfg = dfg;
    }

    public void run() {
	log("using the following CDFG as input:");
	log("----------------------------------");
	dfg.generateDot(false);
	log("----------------------------------");
	log("starting HLS restructuring");
	log("detecting if arrays can be turned into streams");
	ArrayStreamDetector arrayStream = new ArrayStreamDetector(dfg);
	arrayStream.analyze();
	arrayStream.apply();
	log("defining unrolling factor for nested loops");
	NestedLoopUnrolling loopUnfolding = new NestedLoopUnrolling(dfg);
	loopUnfolding.analyze();
	loopUnfolding.apply();
	log("finished HLS restructuring");
    }

    public static void log(String msg) {
	System.out.println("HLS: " + msg);
    }
}
