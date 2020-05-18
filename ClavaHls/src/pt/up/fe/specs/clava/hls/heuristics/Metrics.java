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

import pt.up.fe.specs.clava.analysis.flow.data.DataFlowSubgraphMetrics;

public class Metrics {
    public static DataFlowSubgraphMetrics merge(DataFlowSubgraphMetrics... metrics) {
	DataFlowSubgraphMetrics merged = new DataFlowSubgraphMetrics(null);
	int nStores = 0;
	int nLoads = 0;
	int nOps = 0;
	for (DataFlowSubgraphMetrics metricsObj : metrics) {
	    nStores += metricsObj.getNumArrayStores();
	    nLoads += metricsObj.getNumArrayStores();
	    nOps += metricsObj.getNumOp();
	}
	merged.setNumArrayLoads(nLoads);
	merged.setNumArrayStores(nStores);
	merged.setNumOp(nOps);
	return merged;
    }
}
