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

import java.util.HashMap;

import pt.up.fe.specs.clava.analysis.flow.FlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNodeType;
import pt.up.fe.specs.clava.hls.directives.HLSPipeline;

public class CodeRegionPipelining extends RestructuringStrategy {
    private HashMap<DataFlowNode, Boolean> canPipeline;

    public CodeRegionPipelining(DataFlowGraph dfg) {
	super(dfg);
	canPipeline = new HashMap<>();
    }

    @Override
    public void analyze() {
	for (FlowNode n : dfg.getNodes()) {
	    DataFlowNode node = (DataFlowNode) n;
	    if (node.getType() == DataFlowNodeType.LOOP) {
		// analyze loop with heuristic...
		canPipeline.put(node, true);
	    }
	}

    }

    @Override
    public void apply() {
	canPipeline.forEach((k, v) -> {
	    if (v) {
		HLSPipeline directive = new HLSPipeline();
		insertDirective(k.getStmt(), directive);
	    }
	});
    }

}
