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

package pt.up.fe.specs.clava.hls.heuristics;

import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowParam;
import pt.up.fe.specs.clava.hls.directives.HLSArrayPartition;
import pt.up.fe.specs.clava.hls.directives.HLSArrayPartition.PartitionType;

/**
 * Returns II number for pipeline directive - Integer.MAX_VALUE if II can't be
 * determined, but pipelining is possible - 1..N if II is determined - 0 if
 * pipelining is not possible
 * 
 * @author Tiago
 *
 */
public class PipelineHeuristic {
    private static int limit = 1024 * 4;

    public static int calculate(DataFlowNode node) {
	return Integer.MAX_VALUE;
    }

    public static HLSArrayPartition partition(DataFlowParam p) {
	int nElems = 1;
	for (Integer i : p.getDim())
	    nElems *= i;

	if (p.getDataTypeSize() * nElems <= limit) {
	    HLSArrayPartition dir = new HLSArrayPartition(PartitionType.COMPLETE, p.getName(), -1);
	    return dir;
	} else {
	    // int factor = (nElems * 10) / 1024;
	    int factor = 64;
	    HLSArrayPartition dir = new HLSArrayPartition(PartitionType.CYCLIC, p.getName(), factor);
	    return dir;
	}
    }
}
